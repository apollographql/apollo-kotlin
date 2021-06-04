package com.apollographql.apollo3.internal

import com.apollographql.apollo3.api.ApolloResponse
import com.apollographql.apollo3.api.Operation
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow

expect fun <D: Operation.Data> Flow<ApolloResponse<D>>.maybeFlowOn(dispatcher: CoroutineDispatcher): Flow<ApolloResponse<D>>