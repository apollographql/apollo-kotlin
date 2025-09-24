package com.apollographql.apollo.internal

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

internal actual val defaultDispatcher: CoroutineDispatcher
  get() = Dispatchers.Default
