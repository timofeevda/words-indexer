package indexer.tokenizer

data class Token(val text: String, val position: Int)

/**
 * Iterator over tokens in string
 */
interface Tokenizer {
    fun hasNextToken(): Boolean
    fun nextToken(): Token
}