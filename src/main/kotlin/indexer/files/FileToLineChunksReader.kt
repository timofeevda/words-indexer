package indexer.files

import java.io.File
import java.util.*
import kotlin.collections.ArrayList

data class TextChunk(val final: Boolean, val lines: List<FileLine>)

/**
 * Reads lines from file and provides iterator over the line chunks. The maximum number of
 * lines in chunk is determined by [lineslimit] parameter. End of iteration is
 * additionally signalled with chunk's final flag
 */
class FileToLineChunksReader(private val file: File, private val lineslimit: Int) : AutoCloseable,
    Iterator<TextChunk> {

    private val reader = file.bufferedReader()

    private var lineCount = 0
    private var linesChunk: ArrayList<FileLine> = ArrayList(0)
    private var finalChunk = false

    override fun close() {
        reader.close()
    }

    override fun hasNext(): Boolean {
        val currentFinalChunkState = false

        if (finalChunk) {
            return false
        }

        linesChunk = ArrayList()
        var availableToRead = lineslimit

        var line: String?

        do {
            line = reader.readLine()
            if (line == null) {
                break
            } else {
                lineCount++
                linesChunk.add(FileLine(line, file, lineCount))
                availableToRead--
            }
        } while (availableToRead != 0)

        if (linesChunk.isEmpty()) {
            finalChunk = true
        }

        return !currentFinalChunkState
    }

    override fun next(): TextChunk {
        return TextChunk(
            finalChunk,
            if (linesChunk.isEmpty()) Collections.emptyList() else Collections.unmodifiableList(linesChunk)
        )
    }

}
