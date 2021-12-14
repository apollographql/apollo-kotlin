package com.apollographql.apollo3.testing

import com.apollographql.apollo3.annotations.ApolloExperimental
import kotlinx.coroutines.CoroutineScope
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

@ApolloExperimental
expect fun runTest(
    context: CoroutineContext = EmptyCoroutineContext,
    before: suspend CoroutineScope.() -> Unit = {},
    after: suspend CoroutineScope.() -> Unit = {},
    block: suspend CoroutineScope.() -> Unit,
)