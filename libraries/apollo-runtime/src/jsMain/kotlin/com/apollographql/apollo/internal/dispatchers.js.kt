package com.apollographql.apollo.internal

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import okio.Closeable

internal actual val defaultDispatcher: CoroutineDispatcher
  get() = Dispatchers.Default

