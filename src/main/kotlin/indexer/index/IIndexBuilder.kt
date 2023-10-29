package indexer.index

import java.io.File

interface IIndexBuilder {
    /**
     * Gives access to the word-level inverted index
     */
    val index: IIndex

    /**
     * Builds the word-level inverted index
     *
     * [ioThreads] - number of IO coroutines reading text chunks from files
     *
     * [computationThreads] - number of computation threads splitting text chunks and adding words to index
     *
     * [fileChunksBufferCapacity] - how many file chunks to keep in buffer if all computation coroutines are busy
     *
     * [textLinesLimit] - max number of text lines to read from file and process in one go
     *
     */
    suspend fun buildIndex(ioThreads: Int,
                           computationThreads: Int,
                           fileChunksBufferCapacity: Int,
                           textLinesLimit: Int, progress: suspend (progress: Progress) -> Unit)
}

sealed class Progress

data object StartIndexing : Progress()

data object StopIndexing : Progress()

data class IndexingProgressStep(val file: File, val totalFiles: Long) : Progress() {
    override fun toString() = "IndexingProgressStep(currentFile=${file}, totalFiles=$totalFiles)"
}
