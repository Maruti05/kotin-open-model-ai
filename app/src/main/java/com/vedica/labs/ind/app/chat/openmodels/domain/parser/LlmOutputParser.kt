package com.vedica.labs.ind.app.chat.openmodels.domain.parser

import javax.inject.Inject
import javax.inject.Singleton

enum class SegmentType { TEXT, THINKING, REASONING, TOOL_CALL, CODE_BLOCK }

data class ContentSegment(
    val type: SegmentType,
    val content: String,
    val metadata: Map<String, String> = emptyMap()
)

@Singleton
class LlmOutputParser @Inject constructor() {

    fun parse(
        content: String,
        showThinking: Boolean = true,
        showReasoning: Boolean = true
    ): List<ContentSegment> {
        val segments = mutableListOf<ContentSegment>()
        var remaining = content

        val regexes = listOf(
            Regex("<think>([\\s\\S]*?)</think>") to SegmentType.THINKING,
            Regex("<thinking>([\\s\\S]*?)</thinking>") to SegmentType.THINKING,
            Regex("<reasoning>([\\s\\S]*?)</reasoning>") to SegmentType.REASONING,
            Regex("<tool_call>([\\s\\S]*?)</tool_call>") to SegmentType.TOOL_CALL,
            Regex("<function_call>([\\s\\S]*?)</function_call>") to SegmentType.TOOL_CALL,
            Regex("```(\\w*)\\n([\\s\\S]*?)```") to SegmentType.CODE_BLOCK
        )

        while (remaining.isNotEmpty()) {
            var earliestIndex = Int.MAX_VALUE
            var earliestMatch: MatchResult? = null
            var earliestType: SegmentType? = null

            for ((regex, type) in regexes) {
                val match = regex.find(remaining)
                if (match != null && match.range.first < earliestIndex) {
                    earliestIndex = match.range.first
                    earliestMatch = match
                    earliestType = type
                }
            }

            if (earliestMatch == null) {
                segments.add(ContentSegment(SegmentType.TEXT, remaining))
                break
            }

            // Text before the match
            if (earliestIndex > 0) {
                segments.add(ContentSegment(SegmentType.TEXT, remaining.substring(0, earliestIndex)))
            }

            val matchContent = if (earliestType == SegmentType.CODE_BLOCK) {
                val lang = earliestMatch.groupValues[1]
                val code = earliestMatch.groupValues[2]
                segments.add(ContentSegment(
                    type = SegmentType.CODE_BLOCK,
                    content = code,
                    metadata = mapOf("language" to lang)
                ))
                ""
            } else {
                val inner = earliestMatch.groupValues[1]
                when (earliestType) {
                    SegmentType.THINKING -> if (showThinking) {
                        segments.add(ContentSegment(SegmentType.THINKING, inner))
                    }
                    SegmentType.REASONING -> if (showReasoning) {
                        segments.add(ContentSegment(SegmentType.REASONING, inner))
                    }
                    SegmentType.TOOL_CALL -> segments.add(ContentSegment(SegmentType.TOOL_CALL, inner))
                    else -> {}
                }
                ""
            }

            remaining = remaining.substring(earliestMatch.range.last + 1)
        }

        return segments
    }

    fun cleanOutput(content: String): String {
        return content
            .replace(Regex("<think>[\\s\\S]*?</think>"), "")
            .replace(Regex("<thinking>[\\s\\S]*?</thinking>"), "")
            .replace(Regex("<reasoning>[\\s\\S]*?</reasoning>"), "")
            .replace(Regex("<tool_call>[\\s\\S]*?</tool_call>"), "")
            .replace(Regex("<function_call>[\\s\\S]*?</function_call>"), "")
            .trim()
    }

    fun extractCodeBlocks(content: String): List<Pair<String, String>> {
        val regex = Regex("```(\\w*)\\n([\\s\\S]*?)```")
        return regex.findAll(content).map {
            it.groupValues[1] to it.groupValues[2]
        }.toList()
    }

    fun extractThinking(content: String): List<String> {
        return Regex("<think>([\\s\\S]*?)</think>").findAll(content)
            .map { it.groupValues[1] }
            .toList() + Regex("<thinking>([\\s\\S]*?)</thinking>").findAll(content)
            .map { it.groupValues[1] }
            .toList()
    }

    fun extractToolCalls(content: String): List<String> {
        return Regex("<tool_call>([\\s\\S]*?)</tool_call>").findAll(content)
            .map { it.groupValues[1] }
            .toList() + Regex("<function_call>([\\s\\S]*?)</function_call>").findAll(content)
            .map { it.groupValues[1] }
            .toList()
    }

    fun extractReasoning(content: String): List<String> {
        return Regex("<reasoning>([\\s\\S]*?)</reasoning>").findAll(content)
            .map { it.groupValues[1] }
            .toList()
    }

    fun isStreamingPartialTag(content: String): Boolean {
        return Regex("<(think|thinking|reasoning|tool_call|function_call)[^>]*$").containsMatchIn(content)
    }

    fun sanitizeStreamingContent(content: String): String {
        // Remove incomplete tags at the end of streaming chunks
        return content
            .replace(Regex("<(think|thinking|reasoning|tool_call|function_call)[^>]*$"), "")
            .replace(Regex("</(think|thinking|reasoning|tool_call|function_call)[^>]*$"), "")
    }
}
