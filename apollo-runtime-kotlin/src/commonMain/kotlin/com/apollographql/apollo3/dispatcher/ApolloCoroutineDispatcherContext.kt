package com.apollographql.apollo3.dispatcher

import com.apollographql.apollo3.api.ApolloExperimental
import com.apollographql.apollo3.api.ExecutionContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlin.jvm.JvmField

@ApolloExperimental
class ApolloCoroutineDispatcherContext(
    val default: CoroutineDispatcher
) : ExecutionContext.Element {

  override val key: ExecutionContext.Key<*> = Key

  companion object Key : ExecutionContext.Key<ApolloCoroutineDispatcherContext> {
    @JvmField
    val KEY: Key = Key
  }
}
