package com.apollographql.apollo.execution

import com.apollographql.apollo.execution.internal.LruCache
import kotlinx.atomicfu.locks.reentrantLock
import kotlinx.atomicfu.locks.withLock

class InMemoryPersistedDocumentCache: PersistedDocumentCache {
    private val lock = reentrantLock()
    private val lruCache = LruCache<String, PersistedDocument>(100)

    override fun get(id: String): PersistedDocument? {
        return lock.withLock {
            lruCache.get(id)
        }
    }

    override fun put(id: String, persistedDocument: PersistedDocument) {
        return lock.withLock {
            lruCache.set(id, persistedDocument)
        }
    }
}