package searcher

import indexer.document.Posting
import indexer.document.TermPosting
import indexer.index.WordPostings
import indexer.tokenizer.SimpleTokenizer
import java.io.File
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

data class Result(val file: File, val matches: List<Match>)
data class Match(val terms: List<TermPosting>)

/**
 * Query executor that allows to search word postings for several words. Allows to search
 * phrases consisting of multiple words. For finding consecutive words postings it
 * uses sorted tree to provide efficient searching. [wordDistance] parameter can be
 * used control the level of tolerance for the punctuation symbols between words
 */
class MultiWordSearchQueryExecutor(
    private val wordSearchQueryExecutor: WordSearchQueryExecutor,
    private val wordDistance: Int
) {

    /**
     * Returns the list of results representing the files where matches for the [searchString]
     * were found.
     */
    fun query(searchString: String): List<Result> {
        // special handling for the apostrophe to make apostrophe search possible
        val terms = ArrayList<String>()

        // tokenize search string with specific apostrophe handling
        val tokenizer = SimpleTokenizer(searchString.replace("'", " "))
        while (tokenizer.hasNextToken()) {
            terms.add(tokenizer.nextToken().text)
        }

        val (relevantFiles, partialIndex) = findTermEntries(terms)

        return reduceTermsEntries(terms, relevantFiles, partialIndex)
    }

    /**
     * Returns a pair of relevant file (files containing all terms from the search string)
     * and partial index representing a mapping of words to postings for the words in search string
     */
    private fun findTermEntries(terms: List<String>): Pair<List<File>, Map<String, WordPostings>> {
        val termEntriesMap = HashMap<String, WordPostings>()

        // keep track of files referenced by first term, it doesn't make sense to look through other files referenced
        // by the next terms in the list
        val relevantFiles = ArrayList<File>()

        terms.forEachIndexed { i, term ->
            if (i == 0) {
                val termEntries = wordSearchQueryExecutor.executeQuery(term)
                termEntriesMap[term] = termEntries
                relevantFiles.addAll(termEntries.keys)
            } else {
                val termEntries = wordSearchQueryExecutor.executeQuery(term)
                val filteredTermsMap = HashMap<File, List<Posting>>()
                // filter out files which are not in the list of files which are referenced by previous terms
                termEntries.entries
                    .filter { e -> relevantFiles.contains(e.key) }
                    .forEach { e ->
                        val entries = ArrayList<Posting>()
                        entries.addAll(e.value)
                        filteredTermsMap[e.key] = entries
                    }
                termEntriesMap[term] = filteredTermsMap

                // narrow down the set of relevant files
                relevantFiles.retainAll(filteredTermsMap.keys)
            }
        }

        return Pair(relevantFiles, termEntriesMap)
    }

    /**
     * For each file in the list of [relevantFiles] tries to build a tree of word postings recreating
     * the structure of phrases in text. Phrase is considered as a match in case there is a consecutive
     * matches in tree for all [terms] in search string
     */
    private fun reduceTermsEntries(
        terms: List<String>,
        relevantFiles: List<File>,
        partialIndex: Map<String, WordPostings>
    ): List<Result> {
        val results = ArrayList<Result>()

        for (file in relevantFiles) {

            val treeSet = TreeSet<TermPosting>()

            for (term in terms) {
                partialIndex[term]?.get(file)?.forEach {
                    treeSet.add(TermPosting(term, it.line, it.pos))
                }
            }

            val firstTerm = terms[0]

            val matches = ArrayList<Match>()

            partialIndex[firstTerm]?.get(file)?.forEach {
                var currentTermEntry = TermPosting(firstTerm, it.line, it.pos)

                val termEntries = ArrayList<TermPosting>()
                termEntries.add(currentTermEntry)

                var tailSet = treeSet.tailSet(currentTermEntry)

                for (i in 1 until terms.size) {
                    val nextTerm = terms[i]

                    val termEntry = treeSet.higher(currentTermEntry)

                    // check that the next term from tree is a term corresponding to the one following the previous
                    // term and satisfies word distance rule
                    if (termEntry != null
                        && termEntry.term == nextTerm
                        && calculateWordDistance(currentTermEntry, termEntry) <= wordDistance
                    ) {
                        termEntries.add(termEntry)
                    } else {
                        // doesn't make sense to go to  the next terms, we have already failed to build consecutive
                        // order of terms matching the query
                        break
                    }
                    currentTermEntry = termEntry
                    tailSet = tailSet.tailSet(currentTermEntry)
                }

                // check that we have found the required number of terms matching the original query
                if (termEntries.size == terms.size) {
                    matches.add(Match(termEntries))
                }
            }

            if (matches.isNotEmpty()) {
                results.add(Result(file, matches))
            }
        }
        return results
    }

    private fun calculateWordDistance(currentTermEntry: TermPosting, termEntry: TermPosting): Int =
        when {
            currentTermEntry.line == termEntry.line -> {
                (currentTermEntry.pos + currentTermEntry.term.length) - termEntry.pos
            }
            // adjust position for the multiline case pretending that words are always on the same line
            termEntry.line - currentTermEntry.line == 1 -> {
                val correctedPosition = currentTermEntry.pos + currentTermEntry.term.length + termEntry.pos
                (currentTermEntry.pos + currentTermEntry.term.length) - correctedPosition
            }
            else -> Int.MAX_VALUE
        }

}