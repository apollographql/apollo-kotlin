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


abstract class ApolloCall<D : Operation.Data, E : HasMutableExecutionContext<E>>
(val apolloClient: ApolloClient, val operation: Operation<D>) : HasMutableExecutionContext<E> {
  override var executionContext: ExecutionContext = ExecutionContext.Empty

  override fun addExecutionContext(executionContext: ExecutionContext): E {
    this.executionContext += executionContext
    @Suppress("UNCHECKED_CAST")
    return this as E
  }

  /**
   * Returns a Flow with the results of this [ApolloCall].
   * Note that the execution happens when collecting the Flow.
   * This method can be called several times to execute a call again.
   *
   * Example:
   * ```
   * apolloClient.subscription(NewOrders())
   *                  .toFlow()
   *                  .collect {
   *                    println("order received: ${it.data?.order?.id"})
   *                  }
   * ```
   */
  fun toFlow(): Flow<ApolloResponse<D>> {
    val request = ApolloRequest.Builder(operation)
        .addExecutionContext(executionContext)
        .build()
    return apolloClient.executeAsFlow(request)
  }

  @Deprecated("Please use toFlow instead. This will be removed in v3.0.0.", ReplaceWith("toFlow()"))
  fun executeAsFlow(): Flow<ApolloResponse<D>> = toFlow()
}

/**
 * [ApolloQueryCall] contains everything needed to execute a [Query] with the given [ApolloClient]
 *
 * [ApolloQueryCall] is mutable. You can customize it before calling [execute]
 */
class ApolloQueryCall<D : Query.Data>(apolloClient: ApolloClient, query: Query<D>)
  : ApolloCall<D, ApolloQueryCall<D>>(apolloClient, query) {
  /**
   * Executes the [ApolloQueryCall].
   * [ApolloQueryCall] can be executed several times
   *
   * Example:
   * ```
   * val response = apolloClient.query(HeroQuery())
   *                  .addHttpHeader("Authorization", myToken)
   *                  .fetchPolicy(FetchPolicy.NetworkOnly)
   *                  .execute()
   * ```
   */
  suspend fun execute(): ApolloResponse<D> {
    return toFlow().single()
  }

  fun copy(): ApolloQueryCall<D> {
    return ApolloQueryCall(apolloClient, operation as Query<D>).addExecutionContext(executionContext)
  }
}

/**
 * [ApolloMutationCall] contains everything needed to execute a [Mutation] with the given [ApolloClient]
 *
 * [ApolloMutationCall] is mutable. You can customize it before calling [execute]
 */
class ApolloMutationCall<D : Mutation.Data>(apolloClient: ApolloClient, mutation: Mutation<D>)
  : ApolloCall<D, ApolloMutationCall<D>>(apolloClient, mutation) {
  /**
   * Executes the [ApolloMutationCall].
   * [ApolloMutationCall] can be executed several times
   *
   * Example:
   * ```
   * val response = apolloClient.mutation(SetHeroName("Luke"))
   *                  .addHttpHeader("Authorization", myToken)
   *                  .optimisticData(data)
   *                  .execute()
   * ```
   */
  suspend fun execute(): ApolloResponse<D> {
    return toFlow().single()
  }

  fun copy(): ApolloMutationCall<D> {
    return ApolloMutationCall(apolloClient, operation as Mutation<D>).addExecutionContext(executionContext)
  }
}

/**
 * [ApolloSubscriptionCall] contains everything needed to execute a [Subscription] with the given [ApolloClient]
 *
 * [ApolloSubscriptionCall] is mutable. You can customize it before calling [execute]
 */
class ApolloSubscriptionCall<D : Subscription.Data>(apolloClient: ApolloClient, subscription: Subscription<D>)
  : ApolloCall<D, ApolloSubscriptionCall<D>>(apolloClient, subscription) {

  @Deprecated("Please use toFlow instead. This will be removed in v3.0.0.", ReplaceWith("toFlow()"))
  fun execute(): Flow<ApolloResponse<D>> = toFlow()

  fun copy(): ApolloSubscriptionCall<D> {
    return ApolloSubscriptionCall(apolloClient, operation as Subscription<D>).addExecutionContext(executionContext)
  }
}
