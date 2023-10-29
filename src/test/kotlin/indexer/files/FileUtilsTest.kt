package indexer.files

import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.io.File
import java.nio.file.Paths
import kotlin.math.exp

internal class FileUtilsTest {

    @Test
    @DisplayName("Files are observed in directory")
    fun testObserveFiles() {
        val expectedFileNames = listOf("JavaDemoClass.java", "lorem-ipsum.txt")

        val fileNames = runBlocking {
            collectFilesInDir(Paths.get("./src/test/resources/indexer/fileutils")) {true}.map { it.toFile().name }.toMutableList()
        }

        assertTrue(fileNames.isNotEmpty(), "Files are collected in directory")

        fileNames.removeAll(expectedFileNames)

        assertTrue(fileNames.isEmpty(), "Expected number of files is collected")
    }

    @Test
    @DisplayName("Text chunk lines are observed for file")
    fun testObserveLines() {
        val lines = ArrayList<FileLine>()
        val expectedChunks = ArrayList<TextChunk>()

        runBlocking {
            val chunks = produceTextLineChunks(File("./src/test/resources/indexer/fileutils/java/JavaDemoClass.java"), 100)

            for(chunk in chunks) {
                // working with regular lists is safe here because we are in one thread
                lines.addAll(chunk.lines)
                expectedChunks.add(chunk)
            }
        }

        assertEquals(
            152,
            lines.size,
            "Expected number of lines must be read from file"
        )
        assertTrue(expectedChunks.last().final, "Last chunk should be final one")

    }

    @Test
    @DisplayName("Text lines are split into word entries")
    fun testLineToWordEntries() {
        val text = "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Aenean"

        val expectedWords =
            listOf("lorem", "ipsum", "dolor", "sit", "amet", "consectetur", "adipiscing", "elit", "aenean")
        val expectedPositions = listOf(1, 7, 13, 19, 23, 29, 41, 52, 58)

        val terms = lineToWordEntries(FileLine(text, File("."), 0))

        assertTrue(terms.isNotEmpty(), "\"Line to word\" util function should produce results for non-empty line")

        val words = terms.map { t -> t.token.text }.toMutableList()
        words.removeAll(expectedWords)

        assertTrue(
            words.isEmpty(),
            "\"Line to word\" util function should produce expected number of results for non-empty line"
        )

        val positions = terms.map { t -> t.token.position }.toList()
        assertEquals(
            expectedPositions,
            positions,
            "\"Line to word\" util function should produce expected word positions for non-empty line\""
        )
    }

    @Test
    @DisplayName("Target lines are read skipping intermediate ones")
    fun testLinesReader() {
        val linesMap = readLines(File("./src/test/resources/indexer/fileutils/txt/lorem-ipsum.txt"), setOf(7, 12, 1))
        val expectedTextStarts =
            mapOf(1 to "Lorem ipsum", 7 to "Phasellus nec pretium", 12 to "ante lectus sollicitudin")

        linesMap.entries.forEach { e ->
            assertTrue(
                e.value.startsWith(expectedTextStarts[e.key]!!),
                "Lines read by \"read lines\" utility function should start with expected text"
            )
        }

    }
}