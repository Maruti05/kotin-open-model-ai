package com.vedica.labs.ind.app.chat.openmodels.data.model

enum class ModelFormat(val displayName: String) {
    GGUF("GGUF"),
    LITERT("LiteRT"),
    ONNX("ONNX"),
    UNKNOWN("Unknown");

    companion object {
        fun fromExtension(url: String): ModelFormat {
            val lower = url.lowercase()
            return when {
                lower.endsWith(".gguf") -> GGUF
                lower.endsWith(".tflite") || lower.endsWith(".lite") -> LITERT
                lower.endsWith(".onnx") -> ONNX
                else -> UNKNOWN
            }
        }

        fun fromFileSignature(filePath: String): ModelFormat {
            return try {
                val file = java.io.File(filePath)
                if (!file.exists()) return UNKNOWN
                val raf = java.io.RandomAccessFile(file, "r")
                val magic = ByteArray(4)
                raf.read(magic)
                raf.close()
                val magicHex = magic.joinToString("") { "%02x".format(it) }
                when {
                    magicHex.startsWith("47475546") -> GGUF
                    magicHex.startsWith("1f8b") || magicHex.startsWith("504d") -> LITERT
                    else -> UNKNOWN
                }
            } catch (e: Exception) {
                UNKNOWN
            }
        }

        fun fromBackendType(backendType: BackendType): ModelFormat {
            return when (backendType) {
                BackendType.LLAMA_CPP -> GGUF
                BackendType.LITERT -> LITERT
            }
        }
    }
}
