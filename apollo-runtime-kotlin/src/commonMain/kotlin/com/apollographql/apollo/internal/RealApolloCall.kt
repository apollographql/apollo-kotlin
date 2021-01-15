package com.apollographql.apollo.internal

import com.apollographql.apollo.ApolloMutationCall
import com.apollographql.apollo.ApolloQueryCall
import com.apollographql.apollo.ApolloSubscriptionCall
import com.apollographql.apollo.api.ApolloExperimental
import com.apollographql.apollo.api.Operation
import com.apollographql.apollo.api.Response
import com.apollographql.apollo.api.CustomScalarAdapters
import com.apollographql.apollo.ApolloRequest
import com.apollographql.apollo.interceptor.ApolloRequestInterceptor
import com.apollographql.apollo.interceptor.RealInterceptorChain
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map

@ApolloExperimental
@OptIn(ExperimentalCoroutinesApi::class)
class RealApolloCall<D : Operation.Data> constructor(
    val request: ApolloRequest<D>,
    private val interceptors: List<ApolloRequestInterceptor>,
    private val customScalarAdapters: CustomScalarAdapters
) : ApolloQueryCall<D>, ApolloMutationCall<D>, ApolloSubscriptionCall<D> {

  @ApolloExperimental
  override fun execute(): Flow<Response<D>> {
    return flow {
      emit(
          RealInterceptorChain(
              interceptors,
              0,
              customScalarAdapters
          )
      )
    }.flatMapLatest { interceptorChain ->
      interceptorChain.proceed(request)
    }.map { apolloResponse ->
      apolloResponse.response.copy(
          executionContext = apolloResponse.executionContext
      )
    }
  }
}
