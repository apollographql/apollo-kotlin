package com.apollographql.apollo

import com.apollographql.apollo.api.Response
import kotlinx.coroutines.flow.Flow

interface ApolloCall<T> {
  fun execute(): Flow<Response<T>>
}

interface ApolloQueryCall<T> : ApolloCall<T>

interface ApolloMutationCall<T> : ApolloCall<T>
