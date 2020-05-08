package com.apollographql.apollo.internal

import com.apollographql.apollo.ApolloMutationCall
import com.apollographql.apollo.ApolloQueryCall
import com.apollographql.apollo.executor.ExecutionRequest
import com.apollographql.apollo.api.ApolloExperimental
import com.apollographql.apollo.api.ExecutionContext
import com.apollographql.apollo.api.Operation
import com.apollographql.apollo.api.Response
import com.apollographql.apollo.api.ScalarTypeAdapters
import com.apollographql.apollo.executor.RealRequestExecutorChain
import com.apollographql.apollo.executor.RequestExecutor
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow

@ApolloExperimental
@ExperimentalCoroutinesApi
class RealApolloCall<T> constructor(
    private val operation: Operation<*, T, *>,
    private val scalarTypeAdapters: ScalarTypeAdapters,
    private val executors: List<RequestExecutor>,
    private val executionContext: ExecutionContext
) : ApolloQueryCall<T>, ApolloMutationCall<T> {

  override fun execute(): Flow<Response<T>> {
    val request = ExecutionRequest(
        operation = operation,
        scalarTypeAdapters = scalarTypeAdapters,
        executionContext = executionContext
    )
    return flow {
      emit(RealRequestExecutorChain(executors))
    }.flatMapLatest { executorChain ->
      executorChain.proceed(request)
    }
  }
}
