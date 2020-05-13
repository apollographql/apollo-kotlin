package com.apollographql.apollo.network

import com.apollographql.apollo.api.ApolloExperimental

@ApolloExperimental
expect class ApolloHttpNetworkTransport constructor(
    serverUrl: String,
    httpHeaders: Map<String, String>,
    httpMethod: HttpMethod = HttpMethod.Post
) : NetworkTransport
