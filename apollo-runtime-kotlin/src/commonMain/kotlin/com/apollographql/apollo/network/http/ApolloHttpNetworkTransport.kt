package com.apollographql.apollo.network.http

import com.apollographql.apollo.api.ApolloExperimental
import com.apollographql.apollo.network.HttpMethod
import com.apollographql.apollo.network.NetworkTransport

@ApolloExperimental
expect class ApolloHttpNetworkTransport constructor(
    serverUrl: String,
    headers: Map<String, String>,
    httpMethod: HttpMethod = HttpMethod.Post,
    /**
     * The timeout interval to use when waiting for additional data.
     *
     * - on iOS, it is used to set NSURLSessionConfiguration.timeoutIntervalForRequest
     * - on Android, it is used to set both readTimeout and connectTimeout
     */
    timeoutMillis: Long = 30_000
) : NetworkTransport
