package com.apollographql.apollo3.testing.internal

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.promise
import kotlin.coroutines.CoroutineContext

@OptIn(DelicateCoroutinesApi::class)
internal actual fun runBlockingOrPromise(
    context: CoroutineContext,
    block: suspend CoroutineScope.() -> Unit,
): dynamic = GlobalScope.promise(context = context, block = block)
