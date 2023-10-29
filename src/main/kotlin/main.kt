package indexer

import indexer.index.IndexManager
import indexer.index.IndexingProgressStep
import indexer.index.StartIndexing
import indexer.index.StopIndexing
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.newSingleThreadContext
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import mu.KotlinLogging
import searcher.MultiWordSearchQueryExecutor
import searcher.WordSearchQueryExecutor
import java.util.concurrent.atomic.AtomicInteger


private val logger = KotlinLogging.logger {}

fun main() {
    val indexManager = IndexManager("./src/test") {
        val name = it.toFile().name
        name.endsWith(".java") || name.endsWith(".txt")
    }

    val progressIndicatorDispatcher = newSingleThreadContext("ProgressIndicator")

    val processedFilesCounter = AtomicInteger(0)

    runBlocking {
        launch(Dispatchers.IO) {
            indexManager.buildIndex(
                ioThreads = 20,
                computationThreads = 4,
                fileChunksBufferCapacity = 256,
                textLinesLimit = 1000
            ) {
                withContext(progressIndicatorDispatcher) {
                    when(it) {
                        is IndexingProgressStep -> logger.info { "Progress: ${processedFilesCounter.incrementAndGet()} out of ${it.totalFiles}" }
                        StartIndexing -> logger.info { it }
                        StopIndexing -> logger.info { it }
                    }
                }
            }
        }
    }

    val searchQueryExecutor = WordSearchQueryExecutor(indexManager.index)

    val fullTextSearchQueryExecutor = MultiWordSearchQueryExecutor(searchQueryExecutor, 4)

    val loremIpsum = "efficitur hendrerit tempor justo"

    logger.info { "Results for \"$loremIpsum\": " + fullTextSearchQueryExecutor.query(loremIpsum) }
}











