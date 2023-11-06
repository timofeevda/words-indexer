### Words indexer

Just an example Kotlin coroutines used for implementing words index leveraging structured concurrency. It builds an inverted
index of word postings mapped to files and locations in the file (line, column). IndexManager builds an index concurrently 
using IO threads for reading file chunks from text files, computation threads for splitting text chunks to word postings, 
and putting them concurrently to the words Index. A progress indicator is provided using contextual coroutine callback. 
The index allows concurrent access for searching words. Several implementations of search query 
(exact match, multi-word, word) use Index to provide searching in text files.

See `main.kt` for demo. Indexer function allows to configure the number of IO threads used for traversing directories
and reading files, the number of computation threads to split text lines into words and adding them to index, file
chunks buffer size and the limit of text lines in chunk.

`indexer.searcher` contains a couple of implementations for searching words or phrases (based on distance between word
for tolerating punctuation) in the indexed directory. 
