### Words indexer

Just an example Kotlin coroutines used for implementing words index leveraging structured concurrency. IndexManager builds
an index concurrently using IO threads for reading file chunks from text files, computation threads for splitting text
chunks to word postings, and putting them concurrently to the words Index. A progress indicator is provided using
contextual coroutine callback. The index allows concurrent access for searching words. Several implementations of search
query (exact match, multi-word, word) use Index to provide searching in text files.
