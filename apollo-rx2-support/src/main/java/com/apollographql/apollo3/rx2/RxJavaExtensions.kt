@file:Suppress("NOTHING_TO_INLINE")
@file:JvmName("KotlinExtensions")

package com.apollographql.apollo3.rx2

import com.apollographql.apollo3.ApolloCall
import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.ApolloMutationCall
import com.apollographql.apollo3.ApolloPrefetch
import com.apollographql.apollo3.ApolloQueryCall
import com.apollographql.apollo3.ApolloQueryWatcher
import com.apollographql.apollo3.ApolloSubscriptionCall
import com.apollographql.apollo3.api.Mutation
import com.apollographql.apollo3.api.Operation
import com.apollographql.apollo3.api.Query
import com.apollographql.apollo3.api.ApolloResponse
import com.apollographql.apollo3.api.Subscription
import io.reactivex.BackpressureStrategy
import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.annotations.CheckReturnValue

@JvmSynthetic
@CheckReturnValue
inline fun ApolloPrefetch.rx(): Completable =
    Rx2Apollo.from(this)

@JvmSynthetic
@CheckReturnValue
inline fun <D : Operation.Data> ApolloQueryWatcher<D>.rx(): Observable<ApolloResponse<D>> =
    Rx2Apollo.from(this)

@JvmSynthetic
@CheckReturnValue
inline fun <D : Operation.Data> ApolloCall<D>.rx(): Observable<ApolloResponse<D>> =
    Rx2Apollo.from(this)

@JvmSynthetic
@CheckReturnValue
inline fun <D : Operation.Data> ApolloSubscriptionCall<D>.rx(
    backpressureStrategy: BackpressureStrategy = BackpressureStrategy.LATEST
): Flowable<ApolloResponse<D>> = Rx2Apollo.from(this, backpressureStrategy)

/**
 * Creates a new [ApolloQueryCall] call and then converts it to an [Observable].
 *
 * The number of emissions this Observable will have is based on the
 * [com.apollographql.apollo3.fetcher.ResponseFetcher] used with the call.
 */
@JvmSynthetic
@CheckReturnValue
inline fun <D : Operation.Data> ApolloClient.rxQuery(
    query: Query<D>,
    configure: ApolloQueryCall<D>.() -> ApolloQueryCall<D> = { this }
): Observable<ApolloResponse<D>> = query(query).configure().rx()

/**
 * Creates a new [ApolloMutationCall] call and then converts it to a [Single].
 */
@JvmSynthetic
@CheckReturnValue
inline fun <D : Operation.Data> ApolloClient.rxMutate(
    mutation: Mutation<D>,
    configure: ApolloMutationCall<D>.() -> ApolloMutationCall<D> = { this }
): Single<ApolloResponse<D>> = mutate(mutation).configure().rx().singleOrError()

/**
 * Creates a new [ApolloMutationCall] call and then converts it to a [Single].
 *
 * Provided optimistic updates will be stored in [com.apollographql.apollo3.cache.normalized.ApolloStore]
 * immediately before mutation execution. Any [ApolloQueryWatcher] dependent on the changed cache records will
 * be re-fetched.
 */
@JvmSynthetic
@CheckReturnValue
inline fun <D : Operation.Data> ApolloClient.rxMutate(
    mutation: Mutation<D>,
    withOptimisticUpdates: D,
    configure: ApolloMutationCall<D>.() -> ApolloMutationCall<D> = { this }
): Single<ApolloResponse<D>> = mutate(mutation, withOptimisticUpdates).configure().rx().singleOrError()

/**
 * Creates the [ApolloPrefetch] by wrapping the operation object inside and then converts it to a [Completable].
 */
@JvmSynthetic
@CheckReturnValue
inline fun <D : Operation.Data> ApolloClient.rxPrefetch(
    operation: Operation<D>
): Completable = prefetch(operation).rx()

/**
 * Creates a new [ApolloSubscriptionCall] call and then converts it to a [Flowable].
 *
 * Back-pressure strategy can be provided via [backpressureStrategy] parameter. The default value is [BackpressureStrategy.LATEST]
 */
@JvmSynthetic
@CheckReturnValue
inline fun <D : Operation.Data> ApolloClient.rxSubscribe(
    subscription: Subscription<D>,
    backpressureStrategy: BackpressureStrategy = BackpressureStrategy.LATEST
): Flowable<ApolloResponse<D>> = subscribe(subscription).rx(backpressureStrategy)
