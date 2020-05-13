package com.apollographql.apollo.mock

import com.apollographql.apollo.api.ApolloExperimental
import com.apollographql.apollo.network.GraphQLRequest
import com.apollographql.apollo.network.GraphQLResponse
import com.apollographql.apollo.network.NetworkTransport
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

@ApolloExperimental
@ExperimentalCoroutinesApi
internal class MockNetworkTransport(
    private val mockResponseChannel: Channel<GraphQLResponse> = Channel(capacity = Channel.BUFFERED)
) : NetworkTransport, SendChannel<GraphQLResponse> by mockResponseChannel {

  override fun execute(request: GraphQLRequest): Flow<GraphQLResponse> {
    return flow {
      emit(mockResponseChannel.receive())
    }
  }
}
