package indexer.index

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.newSingleThreadContext
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicReference

internal class IndexManagerTest {

    @Test
    @DisplayName("Index manager should provide expected progress steps")
    fun testProgressSteps() {
        val indexManager = IndexManager("./src/test/resources/indexer/manager") { true }
        val expectedFiles = listOf("index-manager-test.txt", "index-manager-test2.txt")

        val startRef = AtomicReference<StartIndexing>(null)
        val stopRef = AtomicReference<StopIndexing>(null)
        val files = CopyOnWriteArrayList<String>()

        val progressLatch = CountDownLatch(1)

        val progressDispatcher = newSingleThreadContext("Progress")

        runBlocking {
            launch(Dispatchers.IO) {
                indexManager.buildIndex(20, 4, 256, 100) {
                    withContext(progressDispatcher) {
                        when (it) {
                            is StartIndexing -> startRef.set(it)
                            is StopIndexing -> {
                                stopRef.set(it)
                                progressLatch.countDown()
                            }
                            is IndexingProgressStep -> files.add(it.file.name)
                            else -> {
                            }
                        }
                    }
                }
            }
        }

        assertEquals(StartIndexing, startRef.get(), "Index manager reported Start progress state")
        assertEquals(StopIndexing, stopRef.get(), "Index manager reported Stop progress state")

        assertTrue(files.isNotEmpty(), "Progress on files should be reported")

        files.removeAll(expectedFiles)
        assertTrue(files.isEmpty(), "Progress on expected files should be reported")
    }
}