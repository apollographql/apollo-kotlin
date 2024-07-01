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

class Rx3Apollo private constructor() {
  init {
    throw AssertionError("This class cannot be instantiated")
  }

  companion object {
    @JvmStatic
    @CheckReturnValue
    @JvmOverloads
    @Deprecated("use kotlinx-coroutines-rx3 directly", ReplaceWith("call.toFlow().asFlowable(scheduler.asCoroutineDispatcher())"), level = DeprecationLevel.ERROR)
    @ApolloDeprecatedSince(ApolloDeprecatedSince.Version.v4_0_0)
    fun <D : Operation.Data> flowable(call: ApolloCall<D>, scheduler: Scheduler = Schedulers.io()): Flowable<ApolloResponse<D>> {
      return call.toFlow().asFlowable(scheduler.asCoroutineDispatcher())
    }

    @JvmStatic
    @CheckReturnValue
    @JvmOverloads
    @Deprecated("use kotlinx-coroutines-rx3 directly", ReplaceWith("call.toFlow().asFlowable(scheduler.asCoroutineDispatcher()).firstOrError()"), level = DeprecationLevel.ERROR)
    @ApolloDeprecatedSince(ApolloDeprecatedSince.Version.v4_0_0)
    fun <D : Operation.Data> single(call: ApolloCall<D>, scheduler: Scheduler = Schedulers.io()): Single<ApolloResponse<D>> {
      return call.toFlow().asFlowable(scheduler.asCoroutineDispatcher()).firstOrError()
    }
  }
}
