package com.apollographql.apollo3.internal

import com.apollographql.apollo3.ApolloMutationCall
import com.apollographql.apollo3.ApolloQueryCall
import com.apollographql.apollo3.ApolloSubscriptionCall
import com.apollographql.apollo3.api.Operation
import com.apollographql.apollo3.api.ApolloResponse
import com.apollographql.apollo3.api.ResponseAdapterCache
import com.apollographql.apollo3.ApolloRequest
import com.apollographql.apollo3.interceptor.ApolloRequestInterceptor
import com.apollographql.apollo3.interceptor.RealInterceptorChain
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow

@OptIn(ExperimentalCoroutinesApi::class)
class RealApolloCall<D : Operation.Data> constructor(
    val request: ApolloRequest<D>,
    private val interceptors: List<ApolloRequestInterceptor>,
    private val responseAdapterCache: ResponseAdapterCache
) : ApolloQueryCall<D>, ApolloMutationCall<D>, ApolloSubscriptionCall<D> {

    override fun execute(): Flow<ApolloResponse<D>> {
    return flow {
      emit(
          RealInterceptorChain(
              interceptors,
              0,
              responseAdapterCache,
          )
      )
    }.flatMapLatest { interceptorChain ->
      interceptorChain.proceed(request)
    }
  }
}
