package com.apollographql.apollo3.rx2

import com.apollographql.apollo3.api.ApolloRequest
import com.apollographql.apollo3.api.ApolloResponse
import com.apollographql.apollo3.api.Operation
import com.apollographql.apollo3.api.http.HttpRequest
import com.apollographql.apollo3.api.http.HttpResponse
import io.reactivex.Flowable
import io.reactivex.Single


interface Rx2NetworkTransport {
  fun <D : Operation.Data> execute(
      request: ApolloRequest<D>,
  ): Flowable<ApolloResponse<D>>

  fun dispose()
}

interface Rx2HttpInterceptorChain {
  fun proceed(request: HttpRequest): Single<HttpResponse>
}

interface Rx2HttpInterceptor {
  fun intercept(request: HttpRequest, chain: Rx2HttpInterceptorChain): Single<HttpResponse>
}

interface Rx2ApolloInterceptorChain {
  fun <D : Operation.Data> proceed(request: ApolloRequest<D>): Flowable<ApolloResponse<D>>
}

interface Rx2ApolloInterceptor {
  fun <D : Operation.Data> intercept(request: ApolloRequest<D>, chain: Rx2ApolloInterceptorChain): Flowable<ApolloResponse<D>>
}

