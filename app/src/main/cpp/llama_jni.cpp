#include <jni.h>
#include <android/log.h>
#include <string>
#include <vector>
#include <atomic>
#include <thread>
#include <cstring>
#include <ctime>
#include <mutex>
#include <cstdint>
#include <limits>

#include "llama.h"

#define LOG_TAG "LlamaJNI"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

static std::once_flag backend_init_flag;

static void ensure_backend_init() {
    std::call_once(backend_init_flag, []() {
        llama_backend_init();
        LOGI("llama.cpp backend initialized");
    });
}

struct ModelContext {
    llama_model *model = nullptr;
    llama_context *ctx = nullptr;
    std::atomic<bool> stop_requested{false};
    std::atomic<bool> thread_running{false};
    std::thread gen_thread;
};

// ─── nativeLoadModel ───────────────────────────────────────────────
extern "C" JNIEXPORT jlong JNICALL
Java_com_vedica_labs_ind_app_chat_openmodels_domain_inference_GGUFInferenceEngine_nativeLoadModel(
    JNIEnv *env, jobject /*thiz*/,
    jstring j_model_path, jint threads, jint context_size, jint gpu_layers) {

    const char *model_path = env->GetStringUTFChars(j_model_path, nullptr);
    LOGI("Loading model: %s (threads=%d, ctx=%d, gpu=%d)",
         model_path, (int)threads, (int)context_size, (int)gpu_layers);

    ensure_backend_init();

    auto *mctx = new ModelContext();

    llama_model_params model_params = llama_model_default_params();
    model_params.n_gpu_layers = gpu_layers;
    model_params.use_mmap = true;
    model_params.use_mlock = false;

    mctx->model = llama_model_load_from_file(model_path, model_params);
    if (!mctx->model) {
        LOGE("Failed to load model from: %s", model_path);
        env->ReleaseStringUTFChars(j_model_path, model_path);
        delete mctx;
        return 0L;
    }

    llama_context_params ctx_params = llama_context_default_params();
    ctx_params.n_ctx = context_size;
    ctx_params.n_threads = threads;
    ctx_params.n_threads_batch = threads;

    mctx->ctx = llama_init_from_model(mctx->model, ctx_params);
    if (!mctx->ctx) {
        LOGE("Failed to create context");
        llama_model_free(mctx->model);
        env->ReleaseStringUTFChars(j_model_path, model_path);
        delete mctx;
        return 0L;
    }

    env->ReleaseStringUTFChars(j_model_path, model_path);
    LOGI("Model loaded, ctx=%p", (void*)mctx);
    return reinterpret_cast<jlong>(mctx);
}

// ─── nativeGenerateChat ────────────────────────────────────────────
extern "C" JNIEXPORT void JNICALL
Java_com_vedica_labs_ind_app_chat_openmodels_domain_inference_GGUFInferenceEngine_nativeGenerateChat(
    JNIEnv *env, jobject /*thiz*/,
    jlong model_ptr, jstring j_prompt,
    jint max_tokens, jfloat temperature, jfloat top_p, jint top_k,
    jobject j_callback) {

    auto *mctx = reinterpret_cast<ModelContext*>(model_ptr);
    if (!mctx || !mctx->model || !mctx->ctx) {
        LOGE("Invalid model context");
        return;
    }

    const char *prompt = env->GetStringUTFChars(j_prompt, nullptr);
    LOGI("Generating: prompt_len=%zu, max_tokens=%d", strlen(prompt), (int)max_tokens);

    mctx->stop_requested = false;

    jobject callback_global = env->NewGlobalRef(j_callback);
    jclass callback_cls = (jclass)env->NewGlobalRef(env->GetObjectClass(j_callback));
    jmethodID on_token = env->GetMethodID(callback_cls, "onToken", "(Ljava/lang/String;)V");
    jmethodID on_complete = env->GetMethodID(callback_cls, "onComplete", "()V");
    jmethodID on_error = env->GetMethodID(callback_cls, "onError", "(Ljava/lang/String;)V");

    if (!on_token || !on_complete || !on_error) {
        LOGE("Failed to find callback methods");
        env->ReleaseStringUTFChars(j_prompt, prompt);
        env->DeleteGlobalRef(callback_global);
        env->DeleteGlobalRef(callback_cls);
        return;
    }

    const struct llama_vocab *vocab = llama_model_get_vocab(mctx->model);

    int n_tokens = llama_tokenize(vocab, prompt, strlen(prompt), nullptr, 0, true, false);
    if (n_tokens == std::numeric_limits<int32_t>::min()) {
        LOGE("Tokenization overflow");
        jstring err = env->NewStringUTF("Prompt too long for tokenization");
        env->CallVoidMethod(callback_global, on_error, err);
        env->DeleteLocalRef(err);
        env->ReleaseStringUTFChars(j_prompt, prompt);
        env->DeleteGlobalRef(callback_global);
        env->DeleteGlobalRef(callback_cls);
        return;
    }
    if (n_tokens < 0) {
        n_tokens = -n_tokens;
    }
    if (n_tokens == 0) {
        LOGE("Tokenization returned empty result");
        jstring err = env->NewStringUTF("Tokenization failed");
        env->CallVoidMethod(callback_global, on_error, err);
        env->DeleteLocalRef(err);
        env->ReleaseStringUTFChars(j_prompt, prompt);
        env->DeleteGlobalRef(callback_global);
        env->DeleteGlobalRef(callback_cls);
        return;
    }

    std::vector<llama_token> tokens(n_tokens);
    llama_tokenize(vocab, prompt, strlen(prompt), tokens.data(), n_tokens, true, false);
    env->ReleaseStringUTFChars(j_prompt, prompt);

    JavaVM *jvm;
    env->GetJavaVM(&jvm);

    mctx->thread_running.store(true);

    mctx->gen_thread = std::thread([mctx, vocab, tokens = std::move(tokens),
                                     max_tokens, temperature, top_p, top_k,
                                     callback_global, callback_cls,
                                     on_token, on_complete, on_error, jvm]() mutable {
        JNIEnv *thread_env;
        jint attach_ret = jvm->AttachCurrentThread(&thread_env, nullptr);
        if (attach_ret != JNI_OK) {
            LOGE("Failed to attach JNI thread");
            mctx->thread_running.store(false);
            return;
        }

        llama_sampler *smpl = llama_sampler_chain_init(llama_sampler_chain_default_params());
        if (temperature > 0.0f) {
            if (top_k > 0) {
                llama_sampler_chain_add(smpl, llama_sampler_init_top_k(top_k));
            }
            if (top_p > 0.0f && top_p < 1.0f) {
                llama_sampler_chain_add(smpl, llama_sampler_init_top_p(top_p, 1));
            }
            llama_sampler_chain_add(smpl, llama_sampler_init_temp(temperature));
            uint32_t seed = (uint32_t)time(nullptr);
            llama_sampler_chain_add(smpl, llama_sampler_init_dist(seed));
        } else {
            llama_sampler_chain_add(smpl, llama_sampler_init_greedy());
        }

        llama_batch batch = llama_batch_get_one(tokens.data(), (int32_t)tokens.size());
        int decode_ret = llama_decode(mctx->ctx, batch);
        if (decode_ret != 0) {
            LOGE("Prompt evaluation failed (ret=%d)", decode_ret);
            jstring err = thread_env->NewStringUTF("Prompt evaluation failed");
            thread_env->CallVoidMethod(callback_global, on_error, err);
            thread_env->DeleteLocalRef(err);
            llama_sampler_free(smpl);
            thread_env->DeleteGlobalRef(callback_global);
            thread_env->DeleteGlobalRef(callback_cls);
            jvm->DetachCurrentThread();
            mctx->thread_running.store(false);
            return;
        }

        int generated = 0;
        bool first = true;

        while (generated < max_tokens && !mctx->stop_requested.load()) {
            llama_token new_token = llama_sampler_sample(smpl, mctx->ctx, -1);

            if (llama_vocab_is_eog(vocab, new_token)) {
                break;
            }

            char buf[256];
            int n = llama_token_to_piece(vocab, new_token, buf, sizeof(buf), 0, true);
            if (n < 0) continue;
            buf[n] = '\0';

            jstring j_token = thread_env->NewStringUTF(buf);
            thread_env->CallVoidMethod(callback_global, on_token, j_token);
            thread_env->DeleteLocalRef(j_token);

            llama_batch next_batch = llama_batch_get_one(&new_token, 1);
            if (llama_decode(mctx->ctx, next_batch) != 0) {
                LOGE("Decode failed at token %d", generated);
                break;
            }
            generated++;
        }

        if (mctx->stop_requested.load()) {
            jstring msg = thread_env->NewStringUTF("Generation stopped");
            thread_env->CallVoidMethod(callback_global, on_error, msg);
            thread_env->DeleteLocalRef(msg);
        } else {
            thread_env->CallVoidMethod(callback_global, on_complete);
        }

        llama_sampler_free(smpl);
        thread_env->DeleteGlobalRef(callback_global);
        thread_env->DeleteGlobalRef(callback_cls);
        jvm->DetachCurrentThread();
        mctx->thread_running.store(false);
        LOGI("Generated %d tokens", generated);
    });
    mctx->gen_thread.detach();
}

// ─── nativeStopGeneration ──────────────────────────────────────────
extern "C" JNIEXPORT void JNICALL
Java_com_vedica_labs_ind_app_chat_openmodels_domain_inference_GGUFInferenceEngine_nativeStopGeneration(
    JNIEnv *env, jobject /*thiz*/, jlong model_ptr) {

    auto *mctx = reinterpret_cast<ModelContext*>(model_ptr);
    if (mctx) {
        mctx->stop_requested.store(true);
        LOGI("Stop generation requested");
    }
}

// ─── nativeUnloadModel ─────────────────────────────────────────────
extern "C" JNIEXPORT void JNICALL
Java_com_vedica_labs_ind_app_chat_openmodels_domain_inference_GGUFInferenceEngine_nativeUnloadModel(
    JNIEnv *env, jobject /*thiz*/, jlong model_ptr) {

    auto *mctx = reinterpret_cast<ModelContext*>(model_ptr);
    if (!mctx) return;

    LOGI("Unloading model");
    mctx->stop_requested.store(true);

    // Wait for generation thread to finish
    for (int i = 0; i < 100 && mctx->thread_running.load(); i++) {
        std::this_thread::sleep_for(std::chrono::milliseconds(10));
    }
    if (mctx->thread_running.load()) {
        LOGE("Generation thread did not stop in time, proceeding anyway");
    }

    if (mctx->ctx) {
        llama_free(mctx->ctx);
        mctx->ctx = nullptr;
    }
    if (mctx->model) {
        llama_model_free(mctx->model);
        mctx->model = nullptr;
    }

    delete mctx;
    LOGI("Model unloaded");
}
