package com.apollographql.apollo3.dispatcher

import com.apollographql.apollo3.api.ClientContext
import com.apollographql.apollo3.api.ExecutionContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlin.jvm.JvmField

class ApolloCoroutineDispatcher(
    val coroutineDispatcher: CoroutineDispatcher
) : ClientContext(ApolloCoroutineDispatcher) {
  companion object Key : ExecutionContext.Key<ApolloCoroutineDispatcher>
}
