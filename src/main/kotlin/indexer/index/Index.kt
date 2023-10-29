package indexer.index

import indexer.document.Posting
import java.io.File
import java.io.Serializable
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue

class Index : IIndex, Serializable {
    private val wordPostingMap = ConcurrentHashMap<String, ConcurrentHashMap<File, ConcurrentLinkedQueue<Posting>>>()

    override fun put(word: Word) {
        val fileToPostings =
            wordPostingMap.computeIfAbsent(word.token.text) { ConcurrentHashMap<File, ConcurrentLinkedQueue<Posting>>() }
        fileToPostings.computeIfAbsent(word.file) { ConcurrentLinkedQueue<Posting>() }
            .add(Posting(word.line, word.token.position))
    }

    override fun get(word: String) = wordPostingMap[word]?.mapValues { it.value.toList()}

    override fun merge(other: Index) {
        other.wordPostingMap.entries.forEach { entry ->
            val fileToPostings = wordPostingMap.computeIfAbsent(entry.key) { ConcurrentHashMap<File, ConcurrentLinkedQueue<Posting>>() }
            entry.value.entries.forEach { fp ->
                fileToPostings.computeIfAbsent(fp.key) { ConcurrentLinkedQueue<Posting>() }.addAll(fp.value)
            }
        }
    }
}