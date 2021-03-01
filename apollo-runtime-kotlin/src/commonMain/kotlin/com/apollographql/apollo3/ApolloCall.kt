package com.apollographql.apollo3

import com.apollographql.apollo3.api.ExecutionContext
import com.apollographql.apollo3.api.Operation
import com.apollographql.apollo3.api.Response
import kotlinx.coroutines.flow.Flow

interface ApolloCall<D: Operation.Data> {
    fun execute(): Flow<Response<D>>
}

interface ApolloQueryCall<D: Operation.Data> : ApolloCall<D>

interface ApolloMutationCall<D: Operation.Data> : ApolloCall<D>

interface ApolloSubscriptionCall<D: Operation.Data> : ApolloCall<D>
