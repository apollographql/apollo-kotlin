@file:Suppress("NOTHING_TO_INLINE")
@file:JvmName("KotlinExtensions")

package com.apollographql.apollo.rx3

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
import io.reactivex.rxjava3.annotations.CheckReturnValue
import io.reactivex.rxjava3.core.BackpressureStrategy
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Flowable
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import org.reactivestreams.Subscription
import javax.management.Query

@JvmSynthetic
@CheckReturnValue
inline fun ApolloPrefetch.rx(): Completable =
    Rx3Apollo.from(this)

@JvmSynthetic
@CheckReturnValue
inline fun <T> ApolloStoreOperation<T>.rx(): Single<T> =
    Rx3Apollo.from(this)

@JvmSynthetic
@CheckReturnValue
inline fun <T> ApolloQueryWatcher<T>.rx(): Observable<Response<T>> =
    Rx3Apollo.from(this)

@JvmSynthetic
@CheckReturnValue
inline fun <T> ApolloCall<T>.rx(): Observable<Response<T>> =
    Rx3Apollo.from(this)

@JvmSynthetic
@CheckReturnValue
inline fun <T> ApolloSubscriptionCall<T>.rx(
    backpressureStrategy: BackpressureStrategy = BackpressureStrategy.LATEST
): Flowable<Response<T>> = Rx3Apollo.from(this, backpressureStrategy)

/**
 * Creates a new [ApolloQueryCall] call and then converts it to an [Observable].
 *
 * The number of emissions this Observable will have is based on the
 * [com.apollographql.apollo.fetcher.ResponseFetcher] used with the call.
 */
@JvmSynthetic
@CheckReturnValue
inline fun <D : Operation.Data, T, V : Operation.Variables> ApolloClient.rxQuery(
    query: Query<D, T, V>,
    configure: ApolloQueryCall<T>.() -> ApolloQueryCall<T> = { this }
): Observable<Response<T>> = query(query).configure().rx()

/**
 * Creates a new [ApolloMutationCall] call and then converts it to a [Single].
 */
@JvmSynthetic
@CheckReturnValue
inline fun <D : Operation.Data, T, V : Operation.Variables> ApolloClient.rxMutate(
    mutation: Mutation<D, T, V>,
    configure: ApolloMutationCall<T>.() -> ApolloMutationCall<T> = { this }
): Single<Response<T>> = mutate(mutation).configure().rx().singleOrError()

/**
 * Creates a new [ApolloMutationCall] call and then converts it to a [Single].
 *
 * Provided optimistic updates will be stored in [com.apollographql.apollo.cache.normalized.ApolloStore]
 * immediately before mutation execution. Any [ApolloQueryWatcher] dependent on the changed cache records will
 * be re-fetched.
 */
@JvmSynthetic
@CheckReturnValue
inline fun <D : Operation.Data, T, V : Operation.Variables> ApolloClient.rxMutate(
    mutation: Mutation<D, T, V>,
    withOptimisticUpdates: D,
    configure: ApolloMutationCall<T>.() -> ApolloMutationCall<T> = { this }
): Single<Response<T>> = mutate(mutation, withOptimisticUpdates).configure().rx().singleOrError()

/**
 * Creates the [ApolloPrefetch] by wrapping the operation object inside and then converts it to a [Completable].
 */
@JvmSynthetic
@CheckReturnValue
inline fun <D : Operation.Data, T, V : Operation.Variables> ApolloClient.rxPrefetch(
    operation: Operation<D, T, V>
): Completable = prefetch(operation).rx()

/**
 * Creates a new [ApolloSubscriptionCall] call and then converts it to a [Flowable].
 *
 * Back-pressure strategy can be provided via [backpressureStrategy] parameter. The default value is [BackpressureStrategy.LATEST]
 */
@JvmSynthetic
@CheckReturnValue
inline fun <D : Operation.Data, T, V : Operation.Variables> ApolloClient.rxSubscribe(
    subscription: Subscription<D, T, V>,
    backpressureStrategy: BackpressureStrategy = BackpressureStrategy.LATEST
): Flowable<Response<T>> = subscribe(subscription).rx(backpressureStrategy)
