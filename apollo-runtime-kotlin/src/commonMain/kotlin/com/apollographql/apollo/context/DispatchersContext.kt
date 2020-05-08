package com.apollographql.apollo.context

import com.apollographql.apollo.api.ApolloExperimental
import com.apollographql.apollo.api.ExecutionContext
import kotlinx.coroutines.CoroutineDispatcher

@ApolloExperimental
class DispatchersContext(
    val ioDispatcher: CoroutineDispatcher
) : ExecutionContext.Element {
  override val key: ExecutionContext.Key<*> = Key

  companion object Key : ExecutionContext.Key<DispatchersContext>
}
