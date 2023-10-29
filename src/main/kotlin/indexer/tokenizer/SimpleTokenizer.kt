package indexer.tokenizer

import java.util.regex.Matcher
import java.util.regex.Pattern

class SimpleTokenizer(string: String) : Tokenizer {

    companion object SimplePattern {
        val pattern: Pattern = Pattern.compile("\\w+")
    }

    private val matcher: Matcher = pattern.matcher(string)

    override fun hasNextToken(): Boolean = matcher.find()

    override fun nextToken(): Token {
        // matcher position is adjusted for implementing non-zero based positions
        return Token(matcher.group().lowercase().intern(), matcher.start() + 1)
    }
}