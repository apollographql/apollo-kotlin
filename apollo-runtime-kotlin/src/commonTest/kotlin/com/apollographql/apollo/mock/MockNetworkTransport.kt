package com.apollographql.apollo.mock

import com.apollographql.apollo.network.NetworkRequest
import com.apollographql.apollo.network.NetworkResponse
import com.apollographql.apollo.network.NetworkTransport
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

@ExperimentalCoroutinesApi
internal class MockNetworkTransport(
    private val mockResponseChannel: Channel<NetworkResponse> = Channel(capacity = Channel.BUFFERED)
) : NetworkTransport, SendChannel<NetworkResponse> by mockResponseChannel {

  override fun execute(request: NetworkRequest): Flow<NetworkResponse> {
    return flow {
      emit(mockResponseChannel.receive())
    }
  }
}
