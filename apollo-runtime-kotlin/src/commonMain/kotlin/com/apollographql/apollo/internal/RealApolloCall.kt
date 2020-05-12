package com.apollographql.apollo.internal

import com.apollographql.apollo.ApolloMutationCall
import com.apollographql.apollo.ApolloQueryCall
import com.apollographql.apollo.interceptor.ApolloRequest
import com.apollographql.apollo.api.ApolloExperimental
import com.apollographql.apollo.api.ExecutionContext
import com.apollographql.apollo.api.Operation
import com.apollographql.apollo.api.Response
import com.apollographql.apollo.api.ScalarTypeAdapters
import com.apollographql.apollo.interceptor.RealInterceptorChain
import com.apollographql.apollo.interceptor.ApolloRequestInterceptor
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow

@ApolloExperimental
@ExperimentalCoroutinesApi
class RealApolloCall<T> constructor(
    private val operation: Operation<*, T, *>,
    private val scalarTypeAdapters: ScalarTypeAdapters,
    private val interceptors: List<ApolloRequestInterceptor>,
    private val executionContext: ExecutionContext
) : ApolloQueryCall<T>, ApolloMutationCall<T> {

  override fun execute(): Flow<Response<T>> {
    val request = ApolloRequest(
        operation = operation,
        scalarTypeAdapters = scalarTypeAdapters,
        executionContext = executionContext
    )
    return flow {
      emit(RealInterceptorChain(interceptors))
    }.flatMapLatest { interceptorChain ->
      interceptorChain.proceed(request)
    }
  }
}
