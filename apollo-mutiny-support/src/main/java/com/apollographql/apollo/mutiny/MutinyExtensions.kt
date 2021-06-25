@file:Suppress("NOTHING_TO_INLINE")
@file:JvmName("KotlinExtensions")

package com.apollographql.apollo.mutiny

import com.apollographql.apollo.ApolloCall
import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.ApolloMutationCall
import com.apollographql.apollo.ApolloPrefetch
import com.apollographql.apollo.ApolloQueryCall
import com.apollographql.apollo.ApolloQueryWatcher
import com.apollographql.apollo.ApolloSubscriptionCall
import com.apollographql.apollo.api.Mutation
import com.apollographql.apollo.api.Operation
import com.apollographql.apollo.api.Query
import com.apollographql.apollo.api.Response
import com.apollographql.apollo.api.Subscription
import com.apollographql.apollo.cache.normalized.ApolloStoreOperation

import io.smallrye.mutiny.Multi
import io.smallrye.mutiny.Uni
import io.smallrye.mutiny.subscription.BackPressureStrategy

@JvmSynthetic
inline fun ApolloPrefetch.mutiny(): Uni<Void> =
    MutinyApollo.from(this)

@JvmSynthetic
inline fun <T> ApolloStoreOperation<T>.mutiny(): Uni<T> =
    MutinyApollo.from(this)

@JvmSynthetic
inline fun <T> ApolloQueryWatcher<T>.mutiny(): Uni<Response<T>> =
    MutinyApollo.from(this)

@JvmSynthetic
inline fun <T> ApolloCall<T>.mutiny(): Uni<Response<T>> =
    MutinyApollo.from(this)

@JvmSynthetic
inline fun <T> ApolloSubscriptionCall<T>.mutiny(
    backpressureStrategy: BackPressureStrategy = BackPressureStrategy.LATEST
): Multi<Response<T>> = MutinyApollo.from(this, backpressureStrategy)

/**
 * Creates a new [ApolloQueryCall] call and then converts it to an [Uni].
 *
 * The number of emissions this Uni will have is based on the
 * [com.apollographql.apollo.fetcher.ResponseFetcher] used with the call.
 */
@JvmSynthetic
inline fun <D : Operation.Data, T, V : Operation.Variables> ApolloClient.mutinyQuery(
    query: Query<D, T, V>,
    configure: ApolloQueryCall<T>.() -> ApolloQueryCall<T> = { this }
): Uni<Response<T>> = query(query).configure().mutiny()

/**
 * Creates a new [ApolloMutationCall] call and then converts it to a [Uni].
 */
@JvmSynthetic
inline fun <D : Operation.Data, T, V : Operation.Variables> ApolloClient.mutinyMutate(
    mutation: Mutation<D, T, V>,
    configure: ApolloMutationCall<T>.() -> ApolloMutationCall<T> = { this }
): Uni<Response<T>> = mutate(mutation).configure().mutiny()

/**
 * Creates a new [ApolloMutationCall] call and then converts it to a [Uni].
 *
 * Provided optimistic updates will be stored in [com.apollographql.apollo.cache.normalized.ApolloStore]
 * immediately before mutation execution. Any [ApolloQueryWatcher] dependent on the changed cache records will
 * be re-fetched.
 */
@JvmSynthetic
inline fun <D : Operation.Data, T, V : Operation.Variables> ApolloClient.mutinyMutate(
    mutation: Mutation<D, T, V>,
    withOptimisticUpdates: D,
    configure: ApolloMutationCall<T>.() -> ApolloMutationCall<T> = { this }
): Uni<Response<T>> = mutate(mutation, withOptimisticUpdates).configure().mutiny()

/**
 * Creates the [ApolloPrefetch] by wrapping the operation object inside and then converts it to a [Uni].
 */
@JvmSynthetic
inline fun <D : Operation.Data, T, V : Operation.Variables> ApolloClient.mutinyPrefetch(
    operation: Operation<D, T, V>
): Uni<Void> = prefetch(operation).mutiny()

/**
 * Creates a new [ApolloSubscriptionCall] call and then converts it to a [Multi].
 *
 * Back-pressure strategy can be provided via [backpressureStrategy] parameter. The default value is [BackPressureStrategy.LATEST]
 */
@JvmSynthetic
inline fun <D : Operation.Data, T, V : Operation.Variables> ApolloClient.mutinySubscribe(
    subscription: Subscription<D, T, V>,
    backpressureStrategy: BackPressureStrategy = BackPressureStrategy.LATEST
): Multi<Response<T>> = subscribe(subscription).mutiny(backpressureStrategy)
