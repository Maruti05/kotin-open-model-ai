package com.vedica.labs.ind.app.chat.openmodels.data.model

enum class BackendType(val engineName: String, val fileExtension: String) {
    LLAMA_CPP("Llamatik (llama.cpp)", ".gguf"),
    LITERT("LiteRT (XNNPACK)", ".tflite");

    companion object {
        fun fromName(name: String): BackendType {
            return entries.firstOrNull { it.name == name } ?: LLAMA_CPP
        }

        fun fromExtension(extension: String): BackendType {
            return when (extension.lowercase()) {
                ".gguf" -> LLAMA_CPP
                ".tflite", ".lite" -> LITERT
                else -> LLAMA_CPP
            }
        }
    }
}
