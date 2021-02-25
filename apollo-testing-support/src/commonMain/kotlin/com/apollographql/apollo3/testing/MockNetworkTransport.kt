package com.apollographql.apollo3.testing

import com.apollographql.apollo3.api.ApolloExperimental
import com.apollographql.apollo3.api.ResponseAdapterCache
import com.apollographql.apollo3.api.ExecutionContext
import com.apollographql.apollo3.api.Operation
import com.apollographql.apollo3.api.fromResponse
import com.apollographql.apollo3.ApolloRequest
import com.apollographql.apollo3.interceptor.ApolloResponse
import com.apollographql.apollo3.network.NetworkTransport
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import okio.ByteString.Companion.encodeUtf8

@ApolloExperimental
@ExperimentalCoroutinesApi
class MockNetworkTransport(
    private val mockResponseChannel: Channel<String> = Channel(capacity = Channel.BUFFERED)
) : NetworkTransport, SendChannel<String> by mockResponseChannel {

  override fun <D : Operation.Data> execute(request: ApolloRequest<D>, responseAdapterCache: ResponseAdapterCache): Flow<ApolloResponse<D>> {
    return flow {
      emit(
          ApolloResponse(
              requestUuid = request.requestUuid,
              response = request.operation.fromResponse(mockResponseChannel.receive().encodeUtf8()),
              executionContext = ExecutionContext.Empty
          )
      )
    }
  }
}
