package com.apollographql.apollo.internal

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO

internal actual val defaultDispatcher: CoroutineDispatcher
  get() = Dispatchers.IO