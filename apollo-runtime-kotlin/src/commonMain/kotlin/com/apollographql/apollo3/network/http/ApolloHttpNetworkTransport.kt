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
     * The timeout interval to use when connecting
     *
     * - on iOS, it is used to set [NSMutableURLRequest.timeoutInterval]
     * - on Android, it is used to set [OkHttpClient.connectTimeout]
     */
    connectTimeoutMillis: Long = 60_000,
    /**
     * The timeout interval to use when waiting for additional data.
     *
     * - on iOS, it is used to set [NSURLSessionConfiguration.timeoutIntervalForRequest]
     * - on Android, it is used to set  [OkHttpClient.readTimeout]
     */
    readTimeoutMillis: Long = 60_000,
) : NetworkTransport
