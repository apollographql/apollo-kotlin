package com.apollographql.apollo.interceptor

import com.apollographql.apollo.api.ApolloExperimental
import com.apollographql.apollo.api.Response
import kotlinx.coroutines.flow.Flow

@ApolloExperimental
interface ApolloInterceptorChain {

  fun <T> proceed(request: ApolloRequest<T>): Flow<Response<T>>

  fun canProceed(): Boolean

}

@ApolloExperimental
interface ApolloRequestInterceptor {
  fun <T> intercept(request: ApolloRequest<T>, interceptorChain: ApolloInterceptorChain): Flow<Response<T>>
}

@ApolloExperimental
internal class RealInterceptorChain private constructor(
    private val interceptors: List<ApolloRequestInterceptor>,
    private val index: Int
) : ApolloInterceptorChain {

  constructor(interceptors: List<ApolloRequestInterceptor>) : this(
      interceptors = interceptors,
      index = 0
  )

  override fun <T> proceed(request: ApolloRequest<T>): Flow<Response<T>> {
    check(index < interceptors.size)
    return interceptors[index].intercept(request, RealInterceptorChain(interceptors = interceptors, index = index + 1))
  }

  override fun canProceed(): Boolean = index < interceptors.size
}
