package com.apollographql.apollo3

import com.apollographql.apollo3.api.ApolloRequest
import com.apollographql.apollo3.api.ApolloResponse
import com.apollographql.apollo3.api.ExecutionContext
import com.apollographql.apollo3.api.ExecutionOptions
import com.apollographql.apollo3.api.MutableExecutionOptions
import com.apollographql.apollo3.api.Mutation
import com.apollographql.apollo3.api.Operation
import com.apollographql.apollo3.api.Query
import com.apollographql.apollo3.api.Subscription
import com.apollographql.apollo3.api.http.HttpHeader
import com.apollographql.apollo3.api.http.HttpMethod
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.single

abstract class ApolloCall<D : Operation.Data, E>(val apolloClient: ApolloClient, val operation: Operation<D>)
  : MutableExecutionOptions<E> {
  override var executionContext: ExecutionContext = ExecutionContext.Empty

  override fun addExecutionContext(executionContext: ExecutionContext): E {
    this.executionContext += executionContext
    @Suppress("UNCHECKED_CAST")
    return this as E
  }

  override var httpMethod: HttpMethod? = null

  override fun httpMethod(httpMethod: HttpMethod?): E {
    this.httpMethod = httpMethod
    @Suppress("UNCHECKED_CAST")
    return this as E
  }

  override var httpHeaders: List<HttpHeader>? = null

  override fun httpHeaders(httpHeaders: List<HttpHeader>?): E {
    this.httpHeaders = httpHeaders
    @Suppress("UNCHECKED_CAST")
    return this as E
  }

  override fun addHttpHeader(name: String, value: String): E {
    this.httpHeaders = (this.httpHeaders ?: emptyList()) + HttpHeader(name, value)
    @Suppress("UNCHECKED_CAST")
    return this as E
  }

  override var sendApqExtensions: Boolean? = null

  override fun sendApqExtensions(sendApqExtensions: Boolean?): E {
    this.sendApqExtensions = sendApqExtensions
    @Suppress("UNCHECKED_CAST")
    return this as E
  }

  override var sendDocument: Boolean? = null

  override fun sendDocument(sendDocument: Boolean?): E {
    this.sendDocument = sendDocument
    @Suppress("UNCHECKED_CAST")
    return this as E
  }

  override var enableAutoPersistedQueries: Boolean? = null

  override fun enableAutoPersistedQueries(enableAutoPersistedQueries: Boolean?): E  {
    this.enableAutoPersistedQueries = enableAutoPersistedQueries
    @Suppress("UNCHECKED_CAST")
    return this as E
  }

  /**
   * Returns a cold Flow that produces [ApolloResponse]s for this [ApolloCall].
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
        .executionContext(executionContext)
        .httpMethod(httpMethod)
        .httpHeaders(httpHeaders)
        .sendApqExtensions(sendApqExtensions)
        .sendDocument(sendDocument)
        .enableAutoPersistedQueries(enableAutoPersistedQueries)
        .build()
    return apolloClient.executeAsFlow(request)
  }
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

  @Deprecated("Used for backward compatibility with 2.x", ReplaceWith("execute()"))
  suspend fun await(): ApolloResponse<D> = execute()

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

  @Deprecated("Used for backward compatibility with 2.x", ReplaceWith("execute()"))
  suspend fun await(): ApolloResponse<D> = execute()

  fun copy(): ApolloMutationCall<D> {
    return ApolloMutationCall(apolloClient, operation as Mutation<D>).addExecutionContext(executionContext)
  }
}

/**
 * [ApolloSubscriptionCall] contains everything needed to execute a [Subscription] with the given [ApolloClient]
 *
 * [ApolloSubscriptionCall] is mutable. You can customize it before calling [toFlow]
 */
class ApolloSubscriptionCall<D : Subscription.Data>(apolloClient: ApolloClient, subscription: Subscription<D>)
  : ApolloCall<D, ApolloSubscriptionCall<D>>(apolloClient, subscription) {

  fun copy(): ApolloSubscriptionCall<D> {
    return ApolloSubscriptionCall(apolloClient, operation as Subscription<D>).addExecutionContext(executionContext)
  }
}
