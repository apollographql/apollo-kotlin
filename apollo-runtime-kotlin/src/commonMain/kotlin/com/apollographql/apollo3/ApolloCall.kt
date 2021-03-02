package com.apollographql.apollo3

import com.apollographql.apollo3.api.Operation
import com.apollographql.apollo3.api.ApolloResponse
import kotlinx.coroutines.flow.Flow

interface ApolloCall<D: Operation.Data> {
    fun execute(): Flow<ApolloResponse<D>>
}

interface ApolloQueryCall<D: Operation.Data> : ApolloCall<D>

interface ApolloMutationCall<D: Operation.Data> : ApolloCall<D>

interface ApolloSubscriptionCall<D: Operation.Data> : ApolloCall<D>
