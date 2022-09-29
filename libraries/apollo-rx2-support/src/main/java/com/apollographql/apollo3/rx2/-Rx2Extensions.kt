package com.apollographql.apollo3.rx2

import com.apollographql.apollo3.ApolloCall
import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.annotations.ApolloDeprecatedSince
import com.apollographql.apollo3.annotations.ApolloDeprecatedSince.Version.v3_0_0
import com.apollographql.apollo3.api.ApolloResponse
import com.apollographql.apollo3.api.Mutation
import com.apollographql.apollo3.api.Operation
import com.apollographql.apollo3.api.Query
import com.apollographql.apollo3.api.Subscription
import io.reactivex.Flowable
import io.reactivex.Scheduler
import io.reactivex.Single
import io.reactivex.annotations.CheckReturnValue
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.rx2.asCoroutineDispatcher
import kotlinx.coroutines.rx2.asFlowable

@JvmSynthetic
@CheckReturnValue
fun <D: Operation.Data> ApolloCall<D>.rxSingle(scheduler: Scheduler = Schedulers.io()): Single<ApolloResponse<D>>{
  return rxFlowable(scheduler).firstOrError()!!
}

@JvmSynthetic
@CheckReturnValue
fun <D: Operation.Data> ApolloCall<D>.rxFlowable(scheduler: Scheduler = Schedulers.io()): Flowable<ApolloResponse<D>> {
  return toFlow().asFlowable(scheduler.asCoroutineDispatcher())
}


@JvmSynthetic
@CheckReturnValue
@Suppress("UNUSED_PARAMETER", "unused")
@Deprecated(
    message = "Used for backward compatibility with 2.x",
    replaceWith = ReplaceWith("rxFlowable"),
    level = DeprecationLevel.ERROR
)
@ApolloDeprecatedSince(v3_0_0)
fun <D: Operation.Data> ApolloCall<D>.rx(scheduler: Scheduler = Schedulers.io()): Nothing = throw NotImplementedError()

@JvmSynthetic
@CheckReturnValue
@Suppress("UNUSED_PARAMETER", "unused")
@Deprecated(
    message = "Used for backward compatibility with 2.x",
    replaceWith = ReplaceWith("query(query).rxSingle()"),
    level = DeprecationLevel.ERROR
)
@ApolloDeprecatedSince(v3_0_0)
inline fun <D : Query.Data> ApolloClient.rxQuery(
    query: Query<D>,
    configure: ApolloCall<D>.() -> ApolloCall<D> = { this }
): Nothing = throw NotImplementedError()

@JvmSynthetic
@CheckReturnValue
@Suppress("UNUSED_PARAMETER", "unused")
@Deprecated(
    message = "Used for backward compatibility with 2.x",
    replaceWith = ReplaceWith("mutation(mutation).rxSingle()"),
    level = DeprecationLevel.ERROR
)
@ApolloDeprecatedSince(v3_0_0)
inline fun <D : Mutation.Data> ApolloClient.rxMutate(
    mutation: Mutation<D>,
    configure: ApolloCall<D>.() -> ApolloCall<D> = { this }
): Nothing = throw NotImplementedError()

@JvmSynthetic
@CheckReturnValue
@Suppress("UNUSED_PARAMETER", "unused")
@Deprecated(
    message = "Used for backward compatibility with 2.x",
    replaceWith = ReplaceWith("mutation(mutation).rxSingle()"),
    level = DeprecationLevel.ERROR
)
@ApolloDeprecatedSince(v3_0_0)
inline fun <D : Mutation.Data> ApolloClient.rxMutate(
    mutation: Mutation<D>,
    withOptimisticUpdates: D,
    configure: ApolloCall<D>.() -> ApolloCall<D> = { this }
): Nothing = throw NotImplementedError()

@JvmSynthetic
@CheckReturnValue
@Suppress("UNUSED_PARAMETER", "unused")
@Deprecated(
    message = "Used for backward compatibility with 2.x.",
    replaceWith = ReplaceWith("mutation(mutation).rxSingle()"),
    level = DeprecationLevel.ERROR
)
@ApolloDeprecatedSince(v3_0_0)
inline fun <D : Subscription.Data> ApolloClient.rxSubscribe(
    operation: Subscription<D>,
    configure: ApolloCall<D>.() -> ApolloCall<D> = { this }
): Nothing = throw NotImplementedError()

@JvmSynthetic
@CheckReturnValue
@Suppress("UNUSED_PARAMETER", "unused")
@Deprecated(
    message = "3.x doesn't have prefetch anymore. Use a query and ignore the return value",
    level = DeprecationLevel.ERROR
)
@ApolloDeprecatedSince(v3_0_0)
inline fun <D : Operation.Data> ApolloClient.rxPrefetch(
    operation: Operation<D>,
    configure: ApolloCall<D>.() -> ApolloCall<D> = { this }
): Nothing = throw NotImplementedError()

