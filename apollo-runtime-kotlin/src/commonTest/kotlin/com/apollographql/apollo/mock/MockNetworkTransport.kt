package com.apollographql.apollo.mock

import com.apollographql.apollo.api.ApolloExperimental
import com.apollographql.apollo.api.ExecutionContext
import com.apollographql.apollo.api.Operation
import com.apollographql.apollo.interceptor.ApolloRequest
import com.apollographql.apollo.interceptor.ApolloResponse
import com.apollographql.apollo.network.NetworkTransport
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import okio.ByteString.Companion.encodeUtf8

@ApolloExperimental
@ExperimentalCoroutinesApi
internal class MockNetworkTransport(
    private val mockResponseChannel: Channel<String> = Channel(capacity = Channel.BUFFERED)
) : NetworkTransport, SendChannel<String> by mockResponseChannel {

  override fun <D : Operation.Data> execute(request: ApolloRequest<D>, executionContext: ExecutionContext): Flow<ApolloResponse<D>> {
    return flow {
      emit(
          ApolloResponse(
              requestUuid = request.requestUuid,
              response = request.operation.parse(mockResponseChannel.receive().encodeUtf8()),
          )
      )
    }
  }
}
