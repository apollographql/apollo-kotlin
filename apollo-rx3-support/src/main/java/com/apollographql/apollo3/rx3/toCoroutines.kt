/*
 * This file is auto generated from apollo-rx2-support by rxjava3.main.kts, do not edit manually.
 */
@file:JvmName("Rx3Conversion")

package com.apollographql.apollo3.rx3

import com.apollographql.apollo3.api.ApolloRequest
import com.apollographql.apollo3.api.ApolloResponse
import com.apollographql.apollo3.api.Operation
import com.apollographql.apollo3.api.http.HttpRequest
import com.apollographql.apollo3.api.http.HttpResponse
import com.apollographql.apollo3.interceptor.ApolloInterceptor
import com.apollographql.apollo3.interceptor.ApolloInterceptorChain
import com.apollographql.apollo3.network.NetworkTransport
import com.apollographql.apollo3.network.http.HttpInterceptor
import com.apollographql.apollo3.network.http.HttpInterceptorChain
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.rx3.await


fun Rx3NetworkTransport.toNetworkTransport(): NetworkTransport = object : NetworkTransport {
  override fun <D : Operation.Data> execute(request: ApolloRequest<D>): Flow<ApolloResponse<D>> {
    return this@toNetworkTransport.execute(request).asFlow()
  }

  override fun dispose() {
    this@toNetworkTransport.dispose()
  }
}

fun Rx3HttpInterceptorChain.toHttpInterceptorChain(): HttpInterceptorChain = object : HttpInterceptorChain {
  override suspend fun proceed(request: HttpRequest): HttpResponse {
    return this@toHttpInterceptorChain.proceed(request).await()
  }
}

fun Rx3HttpInterceptor.toHttpInterceptor(): HttpInterceptor = object : HttpInterceptor {
  override suspend fun intercept(request: HttpRequest, chain: HttpInterceptorChain): HttpResponse {
    return this@toHttpInterceptor.intercept(request, chain.toRx3HttpInterceptorChain()).await()
  }
}

fun Rx3ApolloInterceptorChain.toApolloInterceptorChain(): ApolloInterceptorChain = object : ApolloInterceptorChain {
  override fun <D : Operation.Data> proceed(request: ApolloRequest<D>): Flow<ApolloResponse<D>> {
    return this@toApolloInterceptorChain.proceed(request).asFlow()
  }
}

fun Rx3ApolloInterceptor.toApolloInterceptor(): ApolloInterceptor = object : ApolloInterceptor {
  override fun <D : Operation.Data> intercept(request: ApolloRequest<D>, chain: ApolloInterceptorChain): Flow<ApolloResponse<D>> {
    return this@toApolloInterceptor.intercept(request, chain.toRx3ApolloInterceptorChain()).asFlow()
  }
}

