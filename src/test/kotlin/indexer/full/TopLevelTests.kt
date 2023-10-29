package indexer.full

import indexer.index.IndexManager
import indexer.searcher.ExactMatchSearchQueryExecutor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import searcher.MultiWordSearchQueryExecutor
import searcher.WordSearchQueryExecutor
import kotlin.test.assertEquals
import kotlin.test.assertTrue


@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class TopLevelTests {

    private lateinit var indexManager: IndexManager
    private lateinit var searchQueryExecutor: WordSearchQueryExecutor
    private lateinit var multiWordQueryExecutor: MultiWordSearchQueryExecutor
    private lateinit var exactMatchSearchQueryExecutor: ExactMatchSearchQueryExecutor

    @BeforeAll
    fun beforeAll() {
        createIndexManager()
        searchQueryExecutor = WordSearchQueryExecutor(indexManager.index)
        multiWordQueryExecutor = MultiWordSearchQueryExecutor(searchQueryExecutor, 4)
        exactMatchSearchQueryExecutor = ExactMatchSearchQueryExecutor(multiWordQueryExecutor)
    }

    @Test
    @DisplayName("Multi-word search in Java code")
    fun testMultiWordJavaSearch() {
        val javaCodeResults = multiWordQueryExecutor.query("subscribeMetrics E entity AbstractTableModel")

        assertTrue(javaCodeResults.isNotEmpty(), "Query search for the Java code shouldn't be empty")
    }

    @Test
    @DisplayName("Multi-word and multi-line search in text files")
    fun testMultiWordMultiLineSearch() {
        val loremIpsumResults = multiWordQueryExecutor.query("hendrerit ante lectus")

        assertTrue(loremIpsumResults.isNotEmpty(), "Query search for the \"lorem ipsum\" should not be empty")
        // two matches for the multiline and single line queries
        assertEquals(
            2,
            loremIpsumResults[0].matches.size,
            "Query search for the \"lorem ipsum\" should return two results"
        )
    }

    @Test
    @DisplayName("Apostrophe handling")
    fun testApostropheSearch() {
        val apostropheResults = multiWordQueryExecutor.query("I'm Batman")

        assertTrue(apostropheResults.isNotEmpty(), "Query search for the apostrophe should not be empty")
    }

    @Test
    @DisplayName("Exact match search test")
    fun testQueries() {
        val exactMatchResults = exactMatchSearchQueryExecutor.query("exact match,. please")
        assertTrue(exactMatchResults.isNotEmpty(), "Exact match should find string with the same punctuation")

        val exactMatchDifferentPunctuationResults = exactMatchSearchQueryExecutor.query("exact match,.! please")
        assertTrue(
            exactMatchDifferentPunctuationResults.isEmpty(),
            "Exact match shouldn't find string with different punctuation"
        )
    }

    private fun createIndexManager() {
        indexManager = IndexManager("./src/test/resources/indexer/queries") { f ->
            val name = f.toFile().name
            name.endsWith(".java") || name.endsWith(".txt")
        }

        runBlocking {
            launch(Dispatchers.IO) {
                indexManager.buildIndex(20, 4, 256 , 1000) {}
            }
        }
    }

}