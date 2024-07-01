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

class Rx2Apollo private constructor() {
  init {
    throw AssertionError("This class cannot be instantiated")
  }

  companion object {
    @JvmStatic
    @CheckReturnValue
    @JvmOverloads
    @Deprecated("use kotlinx-coroutines-rx2 directly", ReplaceWith("call.toFlow().asFlowable(scheduler.asCoroutineDispatcher())"), level = DeprecationLevel.ERROR)
    @ApolloDeprecatedSince(ApolloDeprecatedSince.Version.v4_0_0)
    fun <D : Operation.Data> flowable(call: ApolloCall<D>, scheduler: Scheduler = Schedulers.io()): Flowable<ApolloResponse<D>> {
      return call.toFlow().asFlowable(scheduler.asCoroutineDispatcher())
    }

    @JvmStatic
    @CheckReturnValue
    @JvmOverloads
    @Deprecated("use kotlinx-coroutines-rx2 directly", ReplaceWith("call.toFlow().asFlowable(scheduler.asCoroutineDispatcher()).firstOrError()"), level = DeprecationLevel.ERROR)
    @ApolloDeprecatedSince(ApolloDeprecatedSince.Version.v4_0_0)
    fun <D : Operation.Data> single(call: ApolloCall<D>, scheduler: Scheduler = Schedulers.io()): Single<ApolloResponse<D>> {
      return call.toFlow().asFlowable(scheduler.asCoroutineDispatcher()).firstOrError()
    }
  }
}
