package com.apollographql.apollo3.testing

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

expect fun <T> runBlocking(context: CoroutineContext = EmptyCoroutineContext, block: suspend CoroutineScope.() -> T): T
expect fun <T> runWithMainLoop(context: CoroutineContext = EmptyCoroutineContext, block: suspend CoroutineScope.() -> T): T
expect val MainLoopDispatcher: CoroutineDispatcher