package indexer.files

import indexer.index.Word
import indexer.tokenizer.SimpleTokenizer
import io.reactivex.functions.Predicate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.produce
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import java.io.File
import java.nio.file.Files
import java.nio.file.LinkOption
import java.nio.file.Path
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.io.path.isDirectory

data class FileLine(val text: String, val file: File, val line: Int)

private val dotDirPredicate: Predicate<Path> = Predicate { p -> p.toFile().name.startsWith(".") }

suspend fun collectFilesInDir(dir: Path, predicate: Predicate<Path>): Queue<Path> = coroutineScope {
    val pathsList = ConcurrentLinkedQueue<Path>()
    collectFilesInDir(dir, predicate, pathsList)
    pathsList
}

suspend fun collectFilesInDir(dir: Path, predicate: Predicate<Path>, pathsList: ConcurrentLinkedQueue<Path>): Unit =
    coroutineScope {
        Files.newDirectoryStream(dir).use { ds ->
            for (path in ds) {
                if (path.isDirectory(LinkOption.NOFOLLOW_LINKS) && !dotDirPredicate.test(path)) {
                    launch {
                        collectFilesInDir(path, predicate, pathsList)
                    }
                } else {
                    if (predicate.test(path)) {
                        pathsList.add(path)
                    }
                }
            }
        }
    }

suspend fun CoroutineScope.produceFilesFromQueue(pathsQueue: Queue<Path>) = produce {
    for (path in pathsQueue) {
        send(path)
    }
}

suspend fun CoroutineScope.produceTextLineChunks(file: File, linesLimit: Int) = produce {
    for (chunk in FileToLineChunksReader(file, linesLimit)) {
        send(chunk)
    }
}

/**
 * Converts file line into the list of words
 */
fun lineToWordEntries(line: FileLine): List<Word> {
    val list = ArrayList<Word>()
    val (lineText, file, lineNumber) = line
    val tokenizer = SimpleTokenizer(lineText)
    while (tokenizer.hasNextToken()) {
        list.add(Word(file, lineNumber, tokenizer.nextToken()))
    }
    return list
}

/**
 * Given set of line numbers ([lines]) return map of line number to actual string read from corresponding [file]
 *
 */
fun readLines(file: File, lines: Set<Int>): Map<Int, String> {
    val reader = file.bufferedReader()
    val linesMap = HashMap<Int, String>()
    val sortedLines = lines.sorted()
    var lineRead = 0

    reader.use { r ->
        for (targetLine in sortedLines) {
            // TODO: highly inefficient. It should be implemented based on BufferedReader.skip or RandomAccessFile.seek
            var linesToSkip = targetLine - lineRead - 1
            do {
                r.readLine() ?: return Collections.unmodifiableMap(linesMap)
                linesToSkip--
            } while (linesToSkip != 0)

            val line = r.readLine()
            if (line != null) {
                linesMap[targetLine] = line
            } else {
                return Collections.unmodifiableMap(linesMap)
            }
            lineRead = targetLine
        }
    }

    return Collections.unmodifiableMap(linesMap)
}