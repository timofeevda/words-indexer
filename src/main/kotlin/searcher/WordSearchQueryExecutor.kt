package searcher

import indexer.index.IIndex
import indexer.index.WordPostings
import java.util.Collections.unmodifiableMap

/**
 * Basic query executor that allows to search word postings in files
 */
class WordSearchQueryExecutor(private val index: IIndex) {

    /**
     * Returns word postings map for the given [word]. Word postings map represents
     * mapping of file to the list of word postings in this file
     */
    fun executeQuery(word: String): WordPostings {
        val fileMap = index.get(normalizeQuery(word))
        return if (fileMap == null) mapOf() else unmodifiableMap(fileMap)
    }

    private fun normalizeQuery(word: String) = word.trim().lowercase()
}