/*
 * This file is auto generated from apollo-rx2-support by rxjava3.main.kts, do not edit manually.
 */
package com.apollographql.apollo3.rx3

import com.apollographql.apollo3.api.ApolloRequest
import com.apollographql.apollo3.api.ApolloResponse
import com.apollographql.apollo3.api.Operation
import com.apollographql.apollo3.api.http.HttpRequest
import com.apollographql.apollo3.api.http.HttpResponse
import io.reactivex.rxjava3.core.Flowable
import io.reactivex.rxjava3.core.Single


interface Rx3NetworkTransport {
  fun <D : Operation.Data> execute(
      request: ApolloRequest<D>,
  ): Flowable<ApolloResponse<D>>

  fun dispose()
}

interface Rx3HttpInterceptorChain {
  fun proceed(request: HttpRequest): Single<HttpResponse>
}

interface Rx3HttpInterceptor {
  fun intercept(request: HttpRequest, chain: Rx3HttpInterceptorChain): Single<HttpResponse>
}

interface Rx3ApolloInterceptorChain {
  fun <D : Operation.Data> proceed(request: ApolloRequest<D>): Flowable<ApolloResponse<D>>
}

interface Rx3ApolloInterceptor {
  fun <D : Operation.Data> intercept(request: ApolloRequest<D>, chain: Rx3ApolloInterceptorChain): Flowable<ApolloResponse<D>>
}

