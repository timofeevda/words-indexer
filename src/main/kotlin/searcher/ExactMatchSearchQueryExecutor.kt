package indexer.searcher

import indexer.files.readLines
import searcher.Match
import searcher.MultiWordSearchQueryExecutor
import searcher.Result

/**
 * Query executor that provides exact matches for the query strings. It doesn't rely
 * on word distance threshold used in [MultiWordSearchQueryExecutor] and tries to read the actual strings from
 * file and check if strings contains the exact set of symbols including punctuation.
 *
 * Works only for single-line search
 */
class ExactMatchSearchQueryExecutor(private val multiWordSearchQueryExecutor: MultiWordSearchQueryExecutor) {

    fun query(searchSting: String): List<Result> {
        val results = multiWordSearchQueryExecutor.query(searchSting)
        return results.map { result ->
            val exactMatches = result.matches.filter { m -> isExactMatch(searchSting, result, m) }
            Result(result.file, exactMatches)
        }.filter { result -> result.matches.isNotEmpty() }
    }

    private fun isExactMatch(searchSting: String, r: Result, m: Match): Boolean {
        val linesSet = m.terms.map { t -> t.line }.toSet()
        // multiline exact match is not supported
        if (linesSet.size > 1) {
            return false
        }

        // we are safe for indexing here because there can be no empty matches at this point
        val actualLines = readLines(r.file, setOf(m.terms[0].line))
        val actualLine = actualLines[m.terms[0].line]

        return actualLine?.contains(searchSting) ?: false
    }
}