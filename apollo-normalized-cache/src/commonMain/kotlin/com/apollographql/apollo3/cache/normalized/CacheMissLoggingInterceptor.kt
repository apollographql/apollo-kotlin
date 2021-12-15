package com.apollographql.apollo3.cache.normalized

import com.apollographql.apollo3.interceptor.ApolloInterceptor
import com.apollographql.apollo3.interceptor.ApolloInterceptorChain
import com.apollographql.apollo3.api.ApolloRequest
import com.apollographql.apollo3.api.ApolloResponse
import com.apollographql.apollo3.api.Operation
import com.apollographql.apollo3.exception.CacheMissException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onEach

class CacheMissLoggingInterceptor(private val log: (String) -> Unit) : ApolloInterceptor {
  override fun <D : Operation.Data> intercept(request: ApolloRequest<D>, chain: ApolloInterceptorChain): Flow<ApolloResponse<D>> {
    return chain.proceed(request).onEach {
      if (it.cacheInfo?.missedKey != null) {
        log(
            CacheMissException.message(
                it.cacheInfo?.missedKey,
                it.cacheInfo?.missedField
            )
        )
      }
    }.catch { throwable ->
      if (throwable is CacheMissException) {
        log(throwable.message.toString())
      }
      throw throwable
    }
  }
}