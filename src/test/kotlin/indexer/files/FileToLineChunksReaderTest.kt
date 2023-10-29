package indexer.files

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.io.File

internal class FileToLineChunksReaderTest {

    @Test
    @DisplayName("Test files are read in chunks")
    fun testReadingChunks() {
        val fileReader = FileToLineChunksReader(File("./src/test/resources/indexer/filereader/reader-test-file.txt"), 10)

        val chunks = ArrayList<TextChunk>()
        var finalChunk: TextChunk? = null

        fileReader.use {
            while (fileReader.hasNext()) {
                val chunk = fileReader.next()
                if (!chunk.final) {
                    chunks.add(chunk)
                } else {
                    finalChunk = chunk
                }
            }
        }

        assertEquals(3, chunks.size, "File reader produces right amount of chunks")
        assertNotNull(finalChunk, "Final chunk is present")
        assertEquals(finalChunk!!.final, true, "Final chunk is marked as final")
        assertEquals(0, finalChunk?.lines?.size, "Final chunk contains zero number of lines")
    }
}