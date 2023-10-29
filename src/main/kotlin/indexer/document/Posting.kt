package indexer.document

import java.io.Serializable

private val positionComparator: Comparator<TermPosting> = Comparator.comparingInt { p -> p.pos }
private val lineComparator: Comparator<TermPosting> = Comparator.comparingInt { p -> p.line }

/**
 * Contains word position in document
 */
data class Posting(val line: Int, val pos: Int) : Serializable

/**
 * Posting with custom comparators for being able to build sorted term tree
 */
data class TermPosting(val term: String, val line: Int, val pos: Int) : Comparable<TermPosting> {
    override fun compareTo(other: TermPosting) =
        if (line != other.line) lineComparator.compare(this, other) else positionComparator.compare(this, other)
}