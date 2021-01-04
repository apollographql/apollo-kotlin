package com.apollographql.apollo.internal

import com.apollographql.apollo.ApolloMutationCall
import com.apollographql.apollo.ApolloQueryCall
import com.apollographql.apollo.ApolloSubscriptionCall
import com.apollographql.apollo.api.ApolloExperimental
import com.apollographql.apollo.api.ExecutionContext
import com.apollographql.apollo.api.Operation
import com.apollographql.apollo.api.Response
import com.apollographql.apollo.api.CustomScalarAdapters
import com.apollographql.apollo.interceptor.ApolloRequest
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
    private val operation: Operation<D, *>,
    private val customScalarAdapters: CustomScalarAdapters,
    private val interceptors: List<ApolloRequestInterceptor>,
    private val executionContext: ExecutionContext
) : ApolloQueryCall<D>, ApolloMutationCall<D>, ApolloSubscriptionCall<D> {

  @ApolloExperimental
  override fun execute(executionContext: ExecutionContext): Flow<Response<D>> {
    val request = ApolloRequest(
        operation = operation,
        customScalarAdapters = customScalarAdapters,
        executionContext = this.executionContext + executionContext
    )
    return flow {
      emit(RealInterceptorChain(interceptors))
    }.flatMapLatest { interceptorChain ->
      interceptorChain.proceed(request)
    }.map { apolloResponse ->
      apolloResponse.response.copy(
          executionContext = apolloResponse.executionContext
      )
    }
  }
}
