package com.vedica.labs.ind.app.chat.openmodels.domain.inference

import kotlinx.serialization.json.*
import timber.log.Timber
import java.io.File

class VocabTokenizer private constructor(
    private val idToToken: List<String>,
    private val tokenToId: Map<String, Int>,
    private val vocabSize: Int,
    private val config: BPEConfig
) : Tokenizer {

    data class BPEConfig(
        val merges: List<String>,
        val mergePriority: Map<String, Int>,
        val byteEncoder: Map<Int, String>,
        val byteDecoder: Map<String, Int>,
        val useByteLevel: Boolean,
        val useSentencePieceNormalizer: Boolean,
        val hasByteFallback: Boolean,
        val unkToken: String?,
        val prependPrefix: String?,
        val splitDigits: Boolean
    )

    companion object {
        private const val TAG = "VocabTokenizer"

        @JvmStatic
        private val BYTE_LEVEL_REGEX =
            """'s|'t|'re|'ve|'m|'ll|'d| ?\p{L}+| ?\p{N}+| ?[^\s\p{L}\p{N}]+|\s+(?!\S)|\s+""".toRegex()

        private const val SPACE_CHAR = "\u2581"

        fun load(file: File, hardVocabSize: Int): VocabTokenizer? {
            return try {
                val text = file.readText()
                val json = Json { ignoreUnknownKeys = true }
                val root = json.parseToJsonElement(text).jsonObject
                val modelObj = root["model"]?.jsonObject ?: return null

                val modelType = modelObj["type"]?.jsonPrimitive?.content?.lowercase()
                if (modelType != null && modelType != "bpe") {
                    Timber.tag(TAG).w("Unsupported tokenizer type: %s (only BPE supported)", modelType)
                    return null
                }

                val vocabRaw = parseVocab(modelObj) ?: return null
                val addedTokens = parseAddedTokens(root)
                val mergedVocab = (vocabRaw + addedTokens.map { (k, v) -> k to v })
                    .distinctBy { (_, id) -> id }
                    .sortedBy { (_, id) -> id }

                if (mergedVocab.isEmpty()) return null
                val effective = if (hardVocabSize > 0 && mergedVocab.size > hardVocabSize) {
                    mergedVocab.subList(0, hardVocabSize)
                } else {
                    mergedVocab
                }
                val idToToken = effective.map { it.first }
                val tokenToId = effective.mapIndexed { idx, pair -> pair.first to idx }.toMap()
                val effectiveVocabSize = idToToken.size

                val merges = modelObj["merges"]?.jsonArray?.map { it.jsonPrimitive.content } ?: emptyList()
                val mergePriority = merges.mapIndexed { idx, s -> s to idx }.toMap()

                val preTokenizer = root["pre_tokenizer"]?.jsonObject
                val preType = preTokenizer?.get("type")?.jsonPrimitive?.content

                val hasByteLevelInSub = if (preType == "Sequence") {
                    preTokenizer["pretokenizers"]?.jsonArray?.any { subObj ->
                        subObj.jsonObject["type"]?.jsonPrimitive?.content == "ByteLevel"
                    } ?: false
                } else false
                val useByteLevel = preType == "ByteLevel" || hasByteLevelInSub

                val splitDigits = if (preType == "Sequence") {
                    preTokenizer["pretokenizers"]?.jsonArray?.any { subObj ->
                        subObj.jsonObject["type"]?.jsonPrimitive?.content == "Digits"
                    } ?: false
                } else false

                val normalizer = root["normalizer"]?.jsonObject
                val normType = normalizer?.get("type")?.jsonPrimitive?.content
                val useSentencePieceNormalizer = normType == "Sequence"

                val byteEncoder = bytesToUnicode()
                val byteDecoder = byteEncoder.entries.associate { (k, v) -> v to k }

                val unkTokenStr = modelObj["unk_token"]?.jsonPrimitive?.contentOrNull
                val unkToken = if (unkTokenStr != null && unkTokenStr in tokenToId) unkTokenStr else null

                val hasByteFallback = modelObj["byte_fallback"]?.let {
                    it.jsonPrimitive.content == "true"
                } ?: false

                val config = BPEConfig(
                    merges = merges,
                    mergePriority = mergePriority,
                    byteEncoder = byteEncoder,
                    byteDecoder = byteDecoder,
                    useByteLevel = useByteLevel,
                    useSentencePieceNormalizer = useSentencePieceNormalizer,
                    hasByteFallback = hasByteFallback,
                    unkToken = unkToken,
                    prependPrefix = if (useSentencePieceNormalizer) SPACE_CHAR else null,
                    splitDigits = splitDigits
                )

                Timber.tag(TAG).d(
                    "Loaded BPE tokenizer: vocab=%d, merges=%d, byteLevel=%s, sp=%s, digits=%s, fallback=%s",
                    effectiveVocabSize, merges.size, useByteLevel, useSentencePieceNormalizer,
                    splitDigits, hasByteFallback
                )

                VocabTokenizer(idToToken, tokenToId, effectiveVocabSize, config)
            } catch (e: Exception) {
                Timber.tag(TAG).w(e, "Failed to load tokenizer from %s", file.path)
                null
            }
        }

        private fun parseVocab(modelObj: JsonObject): List<Pair<String, Int>>? {
            val v = modelObj["vocab"] ?: return null
            return when (v) {
                is JsonObject -> {
                    v.toMap().mapNotNull { (token, idElem) ->
                        val id = idElem.jsonPrimitive.content.toIntOrNull() ?: return@mapNotNull null
                        token to id
                    }.sortedBy { it.second }
                }
                is JsonArray -> {
                    v.mapIndexedNotNull { idx, entry ->
                        val arr = entry.jsonArray
                        if (arr.size >= 1) arr[0].jsonPrimitive.content to idx
                        else null
                    }
                }
                else -> null
            }
        }

        private fun parseAddedTokens(root: JsonObject): Map<String, Int> {
            val arr = root["added_tokens"]?.jsonArray ?: return emptyMap()
            return arr.mapNotNull { elem ->
                val obj = elem.jsonObject
                val id = obj["id"]?.jsonPrimitive?.content?.toIntOrNull() ?: return@mapNotNull null
                val content = obj["content"]?.jsonPrimitive?.content ?: return@mapNotNull null
                content to id
            }.toMap()
        }

        @JvmStatic
        fun bytesToUnicode(): Map<Int, String> {
            val bs = mutableListOf<Int>()
            bs.addAll(33..126)
            bs.addAll(161..172)
            bs.addAll(174..255)
            val cs = mutableListOf<Int>()
            for (b in bs) cs.add(b)
            var n = 0
            for (b in 0..255) {
                if (b !in bs) {
                    bs.add(b)
                    cs.add(256 + n)
                    n++
                }
            }
            return bs.zip(cs.map { it.toChar().toString() }).toMap()
        }
    }

    override fun encode(text: String): List<Int> {
        // 1. Normalize
        val processed = if (config.useSentencePieceNormalizer) {
            SPACE_CHAR + text.replace(" ", SPACE_CHAR)
        } else {
            text
        }

        // 2. Pre-tokenize
        val pretokens = if (config.useByteLevel) {
            val raw = BYTE_LEVEL_REGEX.findAll(processed).map { it.value }.toList()
            if (config.splitDigits) raw.flatMap { splitDigitsInToken(it) } else raw
        } else if (config.useSentencePieceNormalizer) {
            processed.split(SPACE_CHAR).filter { it.isNotEmpty() }.map { SPACE_CHAR + it }
        } else {
            listOf(processed)
        }

        // 3. BPE encode each pretoken
        val allTokens = mutableListOf<String>()
        for (pretoken in pretokens) {
            allTokens.addAll(bpeEncode(pretoken))
        }

        // 4. Convert to IDs
        val ids = mutableListOf<Int>()
        for (tokenStr in allTokens) {
            val id = tokenToId[tokenStr]
            if (id != null && id < vocabSize) {
                ids.add(id)
            } else if (config.hasByteFallback) {
                val fallbackIds = toByteFallbackIds(tokenStr)
                for (fid in fallbackIds) {
                    if (fid < vocabSize) ids.add(fid)
                }
            } else {
                config.unkToken?.let { unk ->
                    tokenToId[unk]?.let { uid ->
                        if (uid < vocabSize) ids.add(uid)
                    }
                }
            }
        }

        return ids
    }

    private fun splitDigitsInToken(token: String): List<String> {
        val result = mutableListOf<String>()
        var digits = ""
        var nonDigits = ""
        for (ch in token) {
            if (ch.isDigit()) {
                if (nonDigits.isNotEmpty()) { result.add(nonDigits); nonDigits = "" }
                digits += ch
            } else {
                if (digits.isNotEmpty()) {
                    digits.forEach { result.add(it.toString()) }
                    digits = ""
                }
                nonDigits += ch
            }
        }
        if (digits.isNotEmpty()) digits.forEach { result.add(it.toString()) }
        if (nonDigits.isNotEmpty()) result.add(nonDigits)
        return result
    }

    private fun bpeEncode(token: String): List<String> {
        val word = mutableListOf<String>()
        if (config.useByteLevel) {
            for (b in token.encodeToByteArray()) {
                word.add(config.byteEncoder[b.toInt() and 0xFF] ?: "")
            }
        } else {
            for (ch in token) {
                word.add(ch.toString())
            }
        }

        if (word.size <= 1) return word

        while (true) {
            var bestPair = ""
            var bestRank = Int.MAX_VALUE
            for (i in 0 until word.size - 1) {
                val pair = word[i] + " " + word[i + 1]
                val rank = config.mergePriority[pair] ?: Int.MAX_VALUE
                if (rank < bestRank) {
                    bestRank = rank
                    bestPair = pair
                }
            }
            if (bestPair.isEmpty()) break

            val parts = bestPair.split(" ")
            if (parts.size < 2) break
            val a = parts[0]
            val b = parts[1]

            val newWord = mutableListOf<String>()
            var i = 0
            while (i < word.size) {
                if (i < word.size - 1 && word[i] == a && word[i + 1] == b) {
                    newWord.add(a + b)
                    i += 2
                } else {
                    newWord.add(word[i])
                    i++
                }
            }

            if (newWord.size == word.size) break
            word.clear()
            word.addAll(newWord)
            if (word.size == 1) break
        }

        return word.toList()
    }

    private fun toByteFallbackIds(token: String): List<Int> {
        val ids = mutableListOf<Int>()
        for (ch in token) {
            for (b in ch.toString().encodeToByteArray()) {
                val byteVal = b.toInt() and 0xFF
                val byteStr = "<0x%02X>".format(byteVal)
                tokenToId[byteStr]?.let { ids.add(it) }
            }
        }
        return ids
    }

    override fun decode(tokens: List<Int>): String {
        return if (config.useByteLevel) {
            val bytes = mutableListOf<Byte>()
            for (token in tokens) {
                if (token < 0 || token >= idToToken.size) continue
                val piece = idToToken[token]
                for (ch in piece) {
                    val byteVal = config.byteDecoder[ch.toString()]
                    if (byteVal != null) bytes.add(byteVal.toByte())
                }
            }
            try {
                String(bytes.toByteArray(), Charsets.UTF_8)
            } catch (_: Exception) {
                String(bytes.toByteArray(), Charsets.ISO_8859_1)
            }
        } else {
            val sb = StringBuilder(tokens.size * 4)
            for (token in tokens) {
                if (token < 0 || token >= idToToken.size) continue
                val piece = idToToken[token]
                if (piece.isNotEmpty()) {
                    sb.append(piece.replace(SPACE_CHAR, " "))
                }
            }
            sb.toString().trimStart()
        }
    }
}
