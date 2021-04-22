@file:Suppress("NOTHING_TO_INLINE")
@file:JvmName("KotlinExtensions")

package com.apollographql.apollo.reactor

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
import reactor.core.publisher.Flux
import reactor.core.publisher.FluxSink
import reactor.core.publisher.Mono

@JvmSynthetic
inline fun ApolloPrefetch.reactor(): Mono<Void> =
    ReactorApollo.from(this)

@JvmSynthetic
inline fun <T> ApolloStoreOperation<T>.reactor(): Mono<T> =
    ReactorApollo.from(this)

@JvmSynthetic
inline fun <T> ApolloQueryWatcher<T>.reactor(): Mono<Response<T>> =
    ReactorApollo.from(this)

@JvmSynthetic
inline fun <T> ApolloCall<T>.reactor(): Mono<Response<T>> =
    ReactorApollo.from(this)

@JvmSynthetic
inline fun <T> ApolloSubscriptionCall<T>.reactor(
    backpressureStrategy: FluxSink.OverflowStrategy = FluxSink.OverflowStrategy.LATEST
): Flux<Response<T>> = ReactorApollo.from(this, backpressureStrategy)

/**
 * Creates a new [ApolloQueryCall] call and then converts it to an [Mono].
 *
 * The number of emissions this Mono will have is based on the
 * [com.apollographql.apollo.fetcher.ResponseFetcher] used with the call.
 */
@JvmSynthetic
inline fun <D : Operation.Data, T, V : Operation.Variables> ApolloClient.reactorQuery(
    query: Query<D, T, V>,
    configure: ApolloQueryCall<T>.() -> ApolloQueryCall<T> = { this }
): Mono<Response<T>> = query(query).configure().reactor()

/**
 * Creates a new [ApolloMutationCall] call and then converts it to a [Mono].
 */
@JvmSynthetic
inline fun <D : Operation.Data, T, V : Operation.Variables> ApolloClient.reactorMutate(
    mutation: Mutation<D, T, V>,
    configure: ApolloMutationCall<T>.() -> ApolloMutationCall<T> = { this }
): Mono<Response<T>> = mutate(mutation).configure().reactor()

/**
 * Creates a new [ApolloMutationCall] call and then converts it to a [Mono].
 *
 * Provided optimistic updates will be stored in [com.apollographql.apollo.cache.normalized.ApolloStore]
 * immediately before mutation execution. Any [ApolloQueryWatcher] dependent on the changed cache records will
 * be re-fetched.
 */
@JvmSynthetic
inline fun <D : Operation.Data, T, V : Operation.Variables> ApolloClient.reactorMutate(
    mutation: Mutation<D, T, V>,
    withOptimisticUpdates: D,
    configure: ApolloMutationCall<T>.() -> ApolloMutationCall<T> = { this }
): Mono<Response<T>> = mutate(mutation, withOptimisticUpdates).configure().reactor()

/**
 * Creates the [ApolloPrefetch] by wrapping the operation object inside and then converts it to a [Mono].
 */
@JvmSynthetic
inline fun <D : Operation.Data, T, V : Operation.Variables> ApolloClient.reactorPrefetch(
    operation: Operation<D, T, V>
): Mono<Void> = prefetch(operation).reactor()

/**
 * Creates a new [ApolloSubscriptionCall] call and then converts it to a [Flux].
 *
 * Back-pressure strategy can be provided via [backpressureStrategy] parameter. The default value is [FluxSink.OverflowStrategy.LATEST]
 */
@JvmSynthetic
inline fun <D : Operation.Data, T, V : Operation.Variables> ApolloClient.reactorSubscribe(
    subscription: Subscription<D, T, V>,
    backpressureStrategy: FluxSink.OverflowStrategy = FluxSink.OverflowStrategy.LATEST
): Flux<Response<T>> = subscribe(subscription).reactor(backpressureStrategy)
