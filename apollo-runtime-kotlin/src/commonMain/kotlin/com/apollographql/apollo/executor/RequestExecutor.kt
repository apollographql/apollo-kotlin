package com.apollographql.apollo.executor

import com.apollographql.apollo.api.ApolloExperimental
import com.apollographql.apollo.api.Response
import kotlinx.coroutines.flow.Flow

@ApolloExperimental
interface RequestExecutorChain {

  fun <T> proceed(request: ExecutionRequest<T>): Flow<Response<T>>

  fun canProceed(): Boolean

}

@ApolloExperimental
interface RequestExecutor {
  fun <T> execute(request: ExecutionRequest<T>, executorChain: RequestExecutorChain): Flow<Response<T>>
}

@ApolloExperimental
internal class RealRequestExecutorChain private constructor(
    private val executors: List<RequestExecutor>,
    private val index: Int
) : RequestExecutorChain {

  constructor(executors: List<RequestExecutor>) : this(
      executors = executors,
      index = 0
  )

  override fun <T> proceed(request: ExecutionRequest<T>): Flow<Response<T>> {
    check(index < executors.size)
    return executors[index].execute(request, RealRequestExecutorChain(executors = executors, index = index + 1))
  }

  override fun canProceed(): Boolean = index < executors.size
}
