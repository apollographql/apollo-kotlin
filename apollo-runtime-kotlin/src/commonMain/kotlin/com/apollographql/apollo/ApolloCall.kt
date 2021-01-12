package com.apollographql.apollo

import com.apollographql.apollo.api.ApolloExperimental
import com.apollographql.apollo.api.ExecutionContext
import com.apollographql.apollo.api.Operation
import com.apollographql.apollo.api.Response
import kotlinx.coroutines.flow.Flow

interface ApolloCall<D: Operation.Data> {
  @ApolloExperimental
  fun execute(): Flow<Response<D>>
}

interface ApolloQueryCall<D: Operation.Data> : ApolloCall<D>

interface ApolloMutationCall<D: Operation.Data> : ApolloCall<D>

interface ApolloSubscriptionCall<D: Operation.Data> : ApolloCall<D>
