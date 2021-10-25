package com.apollographql.apollo3

import com.apollographql.apollo3.api.ApolloRequest
import com.apollographql.apollo3.api.ApolloResponse
import com.apollographql.apollo3.api.ExecutionContext
import com.apollographql.apollo3.api.HasMutableExecutionContext
import com.apollographql.apollo3.api.Mutation
import com.apollographql.apollo3.api.Operation
import com.apollographql.apollo3.api.Query
import com.apollographql.apollo3.api.Subscription
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.single


abstract class ApolloCall<D: Operation.Data, E: HasMutableExecutionContext<E>>
(val apolloClient: ApolloClient, val operation: Operation<D>): HasMutableExecutionContext<E> {
  override var executionContext: ExecutionContext = ExecutionContext.Empty

  override fun addExecutionContext(executionContext: ExecutionContext): E {
    this.executionContext += executionContext
    return this as E
  }

  fun executeAsFlow(): Flow<ApolloResponse<D>> {
    val request = ApolloRequest.Builder(operation)
        .addExecutionContext(executionContext)
        .build()
    return apolloClient.executeAsFlow(request)
  }
}

class ApolloQueryCall<D: Query.Data>(apolloClient: ApolloClient, query: Query<D>)
  : ApolloCall<D, ApolloQueryCall<D>>(apolloClient, query) {
  fun copy(): ApolloQueryCall<D> {
    return ApolloQueryCall(apolloClient, operation as Query<D>).addExecutionContext(executionContext)
  }
  suspend fun execute(): ApolloResponse<D> {
    return executeAsFlow().single()
  }
}

class ApolloMutationCall<D: Mutation.Data>(apolloClient: ApolloClient, mutation: Mutation<D>)
  : ApolloCall<D, ApolloMutationCall<D>>(apolloClient, mutation) {
  fun copy(): ApolloMutationCall<D> {
    return ApolloMutationCall(apolloClient, operation as Mutation<D>).addExecutionContext(executionContext)
  }
  suspend fun execute(): ApolloResponse<D> {
    return executeAsFlow().single()
  }
}

class ApolloSubscriptionCall<D: Subscription.Data>(apolloClient: ApolloClient, subscription: Subscription<D>)
  : ApolloCall<D, ApolloSubscriptionCall<D>>(apolloClient, subscription) {
  fun copy(): ApolloSubscriptionCall<D> {
    return ApolloSubscriptionCall(apolloClient, operation as Subscription<D>).addExecutionContext(executionContext)
  }
  fun execute(): Flow<ApolloResponse<D>> {
    return executeAsFlow()
  }
}