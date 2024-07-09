/*
 * This file is auto generated from apollo-rx2-support by rxjava3.main.kts, do not edit manually.
 */
package com.apollographql.apollo.rx3

import com.apollographql.apollo.ApolloCall
import com.apollographql.apollo.annotations.ApolloDeprecatedSince
import com.apollographql.apollo.api.ApolloResponse
import com.apollographql.apollo.api.Operation
import io.reactivex.rxjava3.annotations.CheckReturnValue
import io.reactivex.rxjava3.core.Flowable
import io.reactivex.rxjava3.core.Scheduler
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.schedulers.Schedulers
import kotlinx.coroutines.rx3.asCoroutineDispatcher
import kotlinx.coroutines.rx3.asFlowable

@JvmSynthetic
@CheckReturnValue
@Deprecated("use kotlinx-coroutines-rx3 directly", ReplaceWith("toFlow().asFlowable(scheduler.asCoroutineDispatcher()).firstOrError()"), level = DeprecationLevel.ERROR)
@ApolloDeprecatedSince(ApolloDeprecatedSince.Version.v4_0_0)
fun <D: Operation.Data> ApolloCall<D>.rxSingle(scheduler: Scheduler = Schedulers.io()): Single<ApolloResponse<D>>{
  return toFlow().asFlowable(scheduler.asCoroutineDispatcher()).firstOrError()
}

@JvmSynthetic
@CheckReturnValue
@Deprecated("use kotlinx-coroutines-rx3 directly", ReplaceWith("toFlow.asFlowable(scheduler.asCoroutineDispatcher())"), level = DeprecationLevel.ERROR)
@ApolloDeprecatedSince(ApolloDeprecatedSince.Version.v4_0_0)
fun <D: Operation.Data> ApolloCall<D>.rxFlowable(scheduler: Scheduler = Schedulers.io()): Flowable<ApolloResponse<D>> {
  return toFlow().asFlowable(scheduler.asCoroutineDispatcher())
}

