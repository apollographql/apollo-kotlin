package com.apollographql.apollo.rx2

import com.apollographql.apollo.ApolloCall
import com.apollographql.apollo.annotations.ApolloDeprecatedSince
import com.apollographql.apollo.api.ApolloResponse
import com.apollographql.apollo.api.Operation
import io.reactivex.Flowable
import io.reactivex.Scheduler
import io.reactivex.Single
import io.reactivex.annotations.CheckReturnValue
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.rx2.asCoroutineDispatcher
import kotlinx.coroutines.rx2.asFlowable

@JvmSynthetic
@CheckReturnValue
@Deprecated("use kotlinx-coroutines-rx2 directly", ReplaceWith("toFlow().asFlowable(scheduler.asCoroutineDispatcher()).firstOrError()"), level = DeprecationLevel.ERROR)
@ApolloDeprecatedSince(ApolloDeprecatedSince.Version.v4_0_0)
fun <D: Operation.Data> ApolloCall<D>.rxSingle(scheduler: Scheduler = Schedulers.io()): Single<ApolloResponse<D>>{
  return toFlow().asFlowable(scheduler.asCoroutineDispatcher()).firstOrError()
}

@JvmSynthetic
@CheckReturnValue
@Deprecated("use kotlinx-coroutines-rx2 directly", ReplaceWith("toFlow().asFlowable(scheduler.asCoroutineDispatcher()"), level = DeprecationLevel.ERROR)
@ApolloDeprecatedSince(ApolloDeprecatedSince.Version.v4_0_0)
fun <D: Operation.Data> ApolloCall<D>.rxFlowable(scheduler: Scheduler = Schedulers.io()): Flowable<ApolloResponse<D>> {
  return toFlow().asFlowable(scheduler.asCoroutineDispatcher())
}
