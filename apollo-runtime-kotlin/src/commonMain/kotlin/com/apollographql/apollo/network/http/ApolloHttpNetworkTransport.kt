package com.apollographql.apollo.network.http

import com.apollographql.apollo.api.ApolloExperimental
import com.apollographql.apollo.network.HttpMethod
import com.apollographql.apollo.network.NetworkTransport

@ApolloExperimental
expect class ApolloHttpNetworkTransport constructor(
    serverUrl: String,
    headers: Map<String, String>,
    httpMethod: HttpMethod = HttpMethod.Post
) : NetworkTransport
