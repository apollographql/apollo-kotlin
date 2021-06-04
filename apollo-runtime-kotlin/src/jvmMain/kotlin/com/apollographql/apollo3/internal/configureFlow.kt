package com.apollographql.apollo3.internal

import com.apollographql.apollo3.api.ApolloResponse
import com.apollographql.apollo3.api.Operation
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn

actual fun <D: Operation.Data> Flow<ApolloResponse<D>>.maybeFlowOn(dispatcher: CoroutineDispatcher) = flowOn(dispatcher)