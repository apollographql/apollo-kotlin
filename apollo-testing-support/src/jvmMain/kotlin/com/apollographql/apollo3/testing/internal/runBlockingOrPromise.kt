package com.apollographql.apollo3.testing.internal

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.runBlocking
import kotlin.coroutines.CoroutineContext

internal actual fun runBlockingOrPromise(
    context: CoroutineContext,
    block: suspend CoroutineScope.() -> Unit,
) = runBlocking(context, block)
