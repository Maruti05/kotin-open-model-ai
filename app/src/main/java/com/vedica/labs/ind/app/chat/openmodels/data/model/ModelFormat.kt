package com.vedica.labs.ind.app.chat.openmodels.data.model

enum class ModelFormat {
    GGUF, TFLITE, ONNX, UNKNOWN;

    companion object {
        fun fromExtension(url: String): ModelFormat {
            val lower = url.lowercase()
            return when {
                lower.endsWith(".gguf") -> GGUF
                lower.endsWith(".tflite") -> TFLITE
                lower.endsWith(".onnx") -> ONNX
                else -> UNKNOWN
            }
        }

        fun fromFileSize(sizeBytes: Long): ModelFormat {
            return GGUF
        }
    }
}
