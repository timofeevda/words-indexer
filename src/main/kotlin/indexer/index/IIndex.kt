package indexer.index

import indexer.document.Posting
import indexer.tokenizer.Token
import java.io.File

data class Word(val file: File, val line: Int, val token: Token)

typealias WordPostings = Map<File, List<Posting>>

/**
 * Basic interface for the word-level inverted index
 */
interface IIndex {
    /**
     * Adds corresponding "file to the list of postings" mapping for the given [word]
     */
    fun put(word: Word)

    /**
     * Returns mappings of file to the list of word's postings for the given [word]
     */
    fun get(word: String): WordPostings?

    /**
     * Merge another index mappings into the current one. Another index shouldn't contain word posting duplicates and
     * provide only unique ones
     */
    fun merge(other : Index)
}