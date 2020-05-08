package com.apollographql.apollo.network

import kotlinx.coroutines.flow.Flow

interface NetworkTransport {
  fun execute(request: NetworkRequest): Flow<NetworkResponse>
}
