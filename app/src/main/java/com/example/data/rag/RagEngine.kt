package com.example.data.rag

import kotlin.math.sqrt

data class TextChunk(
    val id: Int,
    val sectionHeader: String,
    val text: String,
    val wordCount: Int,
    val termFrequencies: Map<String, Double>
)

object RagEngine {

    private val STOPWORDS = setOf(
        "a", "about", "above", "after", "again", "against", "all", "am", "an", "and", "any", "are",
        "aren't", "as", "at", "be", "because", "been", "before", "being", "below", "between", "both",
        "but", "by", "can", "cannot", "could", "couldn't", "did", "didn't", "do", "does", "doesn't",
        "doing", "don't", "down", "during", "each", "few", "for", "from", "further", "had", "hadn't",
        "has", "hasn't", "have", "haven't", "having", "he", "her", "here", "hers", "herself", "him",
        "himself", "his", "how", "i", "if", "in", "into", "is", "isn't", "it", "its", "itself", "let's",
        "me", "more", "most", "my", "myself", "no", "nor", "not", "of", "off", "on", "once", "only",
        "or", "other", "ought", "our", "ours", "ourselves", "out", "over", "own", "same", "she",
        "should", "shouldn't", "so", "some", "such", "than", "that", "the", "their", "theirs", "them",
        "themselves", "then", "there", "these", "they", "this", "those", "through", "to", "too", "under",
        "until", "up", "very", "was", "wasn't", "we", "were", "weren't", "what", "when", "where",
        "which", "while", "who", "whom", "why", "with", "won't", "would", "wouldn't", "you", "your"
    )

    fun chunkDocument(rawText: String): List<TextChunk> {
        val lines = rawText.lines()
        val chunks = mutableListOf<TextChunk>()
        var currentHeader = "Overview"
        val currentBuffer = StringBuilder()
        var chunkIndex = 0

        fun flushChunk() {
            val content = currentBuffer.toString().trim()
            if (content.isNotEmpty()) {
                val tf = computeTermFrequency(content)
                chunks.add(
                    TextChunk(
                        id = chunkIndex++,
                        sectionHeader = currentHeader,
                        text = content,
                        wordCount = content.split("\\s+".toRegex()).size,
                        termFrequencies = tf
                    )
                )
                currentBuffer.clear()
            }
        }

        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.startsWith("#")) {
                if (currentBuffer.isNotEmpty()) {
                    flushChunk()
                }
                currentHeader = trimmed.trimStart('#', ' ').ifEmpty { "General Topic" }
            } else if (trimmed.isEmpty()) {
                if (currentBuffer.length > 300) {
                    flushChunk()
                }
            } else {
                currentBuffer.append(line).append("\n")
            }
        }
        flushChunk()

        if (chunks.isEmpty() && rawText.isNotBlank()) {
            val tf = computeTermFrequency(rawText)
            chunks.add(
                TextChunk(
                    id = 0,
                    sectionHeader = "General Material",
                    text = rawText.trim(),
                    wordCount = rawText.split("\\s+".toRegex()).size,
                    termFrequencies = tf
                )
            )
        }
        return chunks
    }

    fun retrieveRelevantChunks(
        documentContent: String,
        query: String,
        topK: Int = 3
    ): List<TextChunk> {
        val allChunks = chunkDocument(documentContent)
        if (allChunks.size <= topK) return allChunks

        val queryTerms = computeTermFrequency(query)
        if (queryTerms.isEmpty()) return allChunks.take(topK)

        val scored = allChunks.map { chunk ->
            val similarity = computeCosineSimilarity(queryTerms, chunk.termFrequencies)
            chunk to similarity
        }

        val sorted = scored.sortedByDescending { it.second }
        val top = sorted.take(topK).map { it.first }
        return if (top.isNotEmpty()) top else allChunks.take(topK)
    }

    private fun computeTermFrequency(text: String): Map<String, Double> {
        val tokens = text.lowercase()
            .replace(Regex("[^a-z0-9\\s]"), " ")
            .split(Regex("\\s+"))
            .filter { it.length > 2 && it !in STOPWORDS }

        if (tokens.isEmpty()) return emptyMap()
        val freq = mutableMapOf<String, Int>()
        for (token in tokens) {
            freq[token] = (freq[token] ?: 0) + 1
        }
        val total = tokens.size.toDouble()
        return freq.mapValues { it.value / total }
    }

    private fun computeCosineSimilarity(
        vecA: Map<String, Double>,
        vecB: Map<String, Double>
    ): Double {
        var dotProduct = 0.0
        var normA = 0.0
        var normB = 0.0

        for ((term, weightA) in vecA) {
            normA += weightA * weightA
            val weightB = vecB[term] ?: 0.0
            dotProduct += weightA * weightB
        }
        for ((_, weightB) in vecB) {
            normB += weightB * weightB
        }

        if (normA == 0.0 || normB == 0.0) return 0.0
        return dotProduct / (sqrt(normA) * sqrt(normB))
    }
}
