@file:Suppress("NOTHING_TO_INLINE")

package com.apollographql.apollo.rx2

import com.apollographql.apollo.*
import com.apollographql.apollo.api.*
import com.apollographql.apollo.cache.normalized.ApolloStoreOperation
import io.reactivex.*

@JvmSynthetic
inline fun ApolloPrefetch.rx(): Completable =
    Rx2Apollo.from(this)

@JvmSynthetic
inline fun <T> ApolloStoreOperation<T>.rx(): Single<T> =
    Rx2Apollo.from(this)

@JvmSynthetic
inline fun <T> ApolloQueryWatcher<T>.rx(): Observable<Response<T>> =
    Rx2Apollo.from(this)

@JvmSynthetic
inline fun <T> ApolloCall<T>.rx(): Observable<Response<T>> =
    Rx2Apollo.from(this)

@JvmSynthetic
inline fun <T> ApolloSubscriptionCall<T>.rx(
    backpressureStrategy: BackpressureStrategy = BackpressureStrategy.LATEST
): Flowable<Response<T>> = Rx2Apollo.from(this, backpressureStrategy)

/**
 * Creates a new [ApolloQueryCall] call and then converts it to an [Observable].
 *
 * The number of emissions this Observable will have is based on the
 * [com.apollographql.apollo.fetcher.ResponseFetcher] used with the call.
 */
@JvmSynthetic
inline fun <D : Operation.Data, T, V : Operation.Variables> ApolloClient.rxQuery(
    query: Query<D, T, V>
): Observable<Response<T>> = query(query).rx()

/**
 * Creates a new [ApolloMutationCall] call and then converts it to a [Single].
 */
@JvmSynthetic
inline fun <D : Operation.Data, T, V : Operation.Variables> ApolloClient.rxMutate(
    mutation: Mutation<D, T, V>
): Single<Response<T>> = mutate(mutation).rx().singleOrError()

/**
 * Creates a new [ApolloMutationCall] call and then converts it to a [Single].
 *
 * Provided optimistic updates will be stored in [com.apollographql.apollo.cache.normalized.ApolloStore]
 * immediately before mutation execution. Any [ApolloQueryWatcher] dependent on the changed cache records will
 * be re-fetched.
 */
@JvmSynthetic
inline fun <D : Operation.Data, T, V : Operation.Variables> ApolloClient.rxMutate(
    mutation: Mutation<D, T, V>,
    withOptimisticUpdates: D
): Single<Response<T>> = mutate(mutation, withOptimisticUpdates).rx().singleOrError()

/**
 * Creates the [ApolloPrefetch] by wrapping the operation object inside and then converts it to a [Completable].
 */
@JvmSynthetic
inline fun <D : Operation.Data, T, V : Operation.Variables> ApolloClient.rxPrefetch(
    operation: Operation<D, T, V>
): Completable = prefetch(operation).rx()

/**
 * Creates a new [ApolloSubscriptionCall] call and then converts it to a [Flowable].
 *
 * Back-pressure strategy can be provided via [backpressureStrategy] parameter. The default value is [BackpressureStrategy.LATEST]
 */
@JvmSynthetic
inline fun <D : Operation.Data, T, V : Operation.Variables> ApolloClient.rxSubscribe(
    subscription: Subscription<D, T, V>,
    backpressureStrategy: BackpressureStrategy = BackpressureStrategy.LATEST
): Flowable<Response<T>> = subscribe(subscription).rx(backpressureStrategy)
