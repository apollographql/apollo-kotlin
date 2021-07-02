package com.apollographql.apollo3.testing

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlin.coroutines.CoroutineContext

actual fun <T> runBlocking(context: CoroutineContext, block: suspend CoroutineScope.() -> T): T {
  TODO()
}

actual fun <T> runWithMainLoop(context: CoroutineContext, block: suspend CoroutineScope.() -> T): T {
  TODO()
}

actual val MainLoopDispatcher: CoroutineDispatcher = Dispatchers.Default