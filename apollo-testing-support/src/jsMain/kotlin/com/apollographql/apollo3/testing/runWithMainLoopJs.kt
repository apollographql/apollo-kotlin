package com.apollographql.apollo3.testing

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.promise
import kotlin.coroutines.CoroutineContext

@OptIn(DelicateCoroutinesApi::class)
actual fun runTest(block: suspend CoroutineScope.() -> Unit): dynamic {
  return GlobalScope.promise { block() }
}

actual fun <T> runBlocking(context: CoroutineContext, block: suspend CoroutineScope.() -> T): T {
  TODO("Can't be done in JS!")
}

actual fun <T> runWithMainLoop(context: CoroutineContext, block: suspend CoroutineScope.() -> T): T {
  return runBlocking(MainLoopDispatcher + context, block)
}

actual val MainLoopDispatcher: CoroutineDispatcher = Dispatchers.Main