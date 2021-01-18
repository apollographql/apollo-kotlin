package com.apollographql.apollo.api.network

/**
 * Used to set Content-Type header in the outgoing request
 */
object ApolloMediaType {
    const val APPLICATION_JSON = "application/json"

    @Deprecated("RFC8259 has deprecated the charset substring. Use application/json")
    const val APPLICATION_JSON_UTF8 = "application/json; charset=utf-8"
}