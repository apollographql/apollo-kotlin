/*
 * This file is auto generated from apollo-rx2-support by rxjava3.main.kts, do not edit manually.
 */
package com.apollographql.apollo3.rx3

import com.apollographql.apollo3.ApolloCall
import com.apollographql.apollo3.api.ApolloResponse
import com.apollographql.apollo3.api.Operation
import io.reactivex.rxjava3.core.Flowable
import io.reactivex.rxjava3.core.Scheduler
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.annotations.CheckReturnValue
import io.reactivex.rxjava3.schedulers.Schedulers

class Rx3Apollo private constructor() {
  init {
    throw AssertionError("This class cannot be instantiated")
  }

  companion object {
    @JvmStatic
    @CheckReturnValue
    @JvmOverloads
    fun <D : Operation.Data> flowable(call: ApolloCall<D>, scheduler: Scheduler = Schedulers.io()): Flowable<ApolloResponse<D>> {
      return call.rxFlowable(scheduler)
    }

    @JvmStatic
    @CheckReturnValue
    @JvmOverloads
    fun <D : Operation.Data> single(call: ApolloCall<D>, scheduler: Scheduler = Schedulers.io()): Single<ApolloResponse<D>> {
      return call.rxSingle(scheduler)
    }

    @JvmStatic
    @CheckReturnValue
    @JvmOverloads
    @Deprecated(
        message = "Used for backward compatibility with 2.x",
        replaceWith = ReplaceWith("flowable(call)"),
        level = DeprecationLevel.ERROR
    )
    fun <D : Operation.Data> from(call: ApolloCall<D>, scheduler: Scheduler = Schedulers.io()): Flowable<ApolloResponse<D>> {
      return call.rxFlowable(scheduler)
    }
  }
}
