package com.apollographql.apollo

import com.apollographql.apollo.api.ApolloExperimental
import com.apollographql.apollo.api.ExecutionContext
import com.apollographql.apollo.api.Response
import kotlinx.coroutines.flow.Flow

interface ApolloCall<T> {
  @ApolloExperimental
  fun execute(executionContext: ExecutionContext = ExecutionContext.Empty): Flow<Response<T>>
}

interface ApolloQueryCall<T> : ApolloCall<T>

interface ApolloMutationCall<T> : ApolloCall<T>

interface ApolloSubscriptionCall<T> : ApolloCall<T>
