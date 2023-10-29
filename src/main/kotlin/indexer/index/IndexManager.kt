package indexer.index

import indexer.files.TextChunk
import indexer.files.collectFilesInDir
import indexer.files.lineToWordEntries
import indexer.files.produceFilesFromQueue
import indexer.files.produceTextLineChunks
import io.reactivex.functions.Predicate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import java.io.File
import java.nio.file.Path
import java.nio.file.Paths
import java.util.concurrent.atomic.AtomicInteger

class IndexManager(
    private val dirToIndex: String,
    private val filePredicate: Predicate<Path>
) : IIndexBuilder {

    private val idx = Index()

    override val index = idx

    override suspend fun buildIndex(
        ioThreads: Int,
        computationThreads: Int,
        fileChunksBufferCapacity: Int,
        textLinesLimit: Int,
        progress: suspend (progress: Progress) -> Unit
    ) = coroutineScope {
        progress.invoke(StartIndexing)

        // get a reference to  a job for being able to cancel it when all file chunk producers finished their work
        val job = coroutineContext.job

        val filesToProcess = collectFilesInDir(Paths.get(dirToIndex), filePredicate)
        val totalFilesCount = filesToProcess.size.toLong()

        val fileChunksChannel = Channel<Pair<File, TextChunk>>(fileChunksBufferCapacity)
        val finishedFileChunksProducers = AtomicInteger(0)

        val pathsProducer = produceFilesFromQueue(filesToProcess)

        // launch IO threads for reading text lines form files
        repeat(ioThreads) {
            launch(Dispatchers.IO) {
                for (path in pathsProducer) {
                    val file = path.toFile()
                    val fileChunks = produceTextLineChunks(file, textLinesLimit)

                    for (chunk in fileChunks) {
                        fileChunksChannel.send(Pair(file, chunk))
                        if (chunk.final) {
                            reportFileProgress(file, totalFilesCount, progress)
                        }
                    }
                }

                if (finishedFileChunksProducers.incrementAndGet() == ioThreads) {
                    progress.invoke(StopIndexing)
                    job.cancel()
                }
            }
        }

        // launch computation thread for working with text lines and updating index with word postings
        repeat(computationThreads) {
            launch(Dispatchers.Default) {
                for (chunk in fileChunksChannel) {
                    val (_, textChunk) = chunk
                    textChunk.lines.forEach { l -> lineToWordEntries(l).forEach { w -> index.put(w) } }
                }
            }
        }

    }

    private suspend fun reportFileProgress(
        file: File,
        total: Long,
        progressTracker: suspend (progress: Progress) -> Unit
    ) {
        progressTracker.invoke(IndexingProgressStep(file, total))
    }
}

