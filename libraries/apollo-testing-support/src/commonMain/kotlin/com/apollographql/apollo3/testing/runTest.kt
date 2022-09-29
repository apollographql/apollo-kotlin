package com.apollographql.apollo3.testing

import com.apollographql.apollo3.annotations.ApolloDeprecatedSince
import com.apollographql.apollo3.annotations.ApolloDeprecatedSince.Version.v3_4_1
import com.apollographql.apollo3.annotations.ApolloExperimental
import kotlinx.coroutines.CoroutineScope
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

@ApolloExperimental
@Deprecated("Use kotlinx.coroutines.test.runTest from org.jetbrains.kotlinx:kotlinx-coroutines-test instead")
@ApolloDeprecatedSince(v3_4_1)
fun runTest(
    context: CoroutineContext = EmptyCoroutineContext,
    before: suspend CoroutineScope.() -> Unit = {},
    after: suspend CoroutineScope.() -> Unit = {},
    block: suspend CoroutineScope.() -> Unit,
) {
  com.apollographql.apollo3.testing.internal.runTest(skipDelays = false, context, before, after, block)
}
