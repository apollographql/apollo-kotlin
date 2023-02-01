package com.apollographql.apollo3.rx2

import com.apollographql.apollo3.ApolloCall
import com.apollographql.apollo3.api.ApolloResponse
import com.apollographql.apollo3.api.Operation
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
