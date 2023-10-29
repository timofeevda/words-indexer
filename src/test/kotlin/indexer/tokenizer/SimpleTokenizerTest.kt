package indexer.tokenizer

import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import kotlin.collections.ArrayList
import kotlin.test.assertTrue

internal class SimpleTokenizerTest {

    @Test
    @DisplayName("Tokenizer produces expected set of terms")
    fun testTokenizer() {
        val textLine = "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Aenean"

        val expectedTerms = listOf("lorem", "ipsum", "dolor", "sit", "amet", "consectetur", "adipiscing", "elit", "aenean")

        val tokenizer = SimpleTokenizer(textLine)
        val tokens = ArrayList<String>()
        while (tokenizer.hasNextToken()) {
            tokens.add(tokenizer.nextToken().text)
        }

        assertTrue(tokens.isNotEmpty(), "Tokenizer should produce results for non-empty string")

        tokens.removeAll(expectedTerms)

        assertTrue(tokens.isEmpty(), "Tokenizer should produce expected list of terms for given string")

    }
}