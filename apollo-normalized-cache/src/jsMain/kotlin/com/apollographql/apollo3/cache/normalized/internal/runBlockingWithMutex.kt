package com.apollographql.apollo3.cache.normalized.internal

import kotlinx.coroutines.sync.Mutex

internal actual fun runBlockingWithMutex(mutex: Mutex, block: () -> Unit) = block()
