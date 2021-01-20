package com.apollographql.apollo.api.internal.network

/**
 * Used to set Content-Type header in the outgoing request
 */
object ContentType {
    const val APPLICATION_JSON = "application/json"

    @Deprecated("RFC8259 has deprecated the charset substring. Use application/json")
    const val APPLICATION_JSON_UTF8 = "application/json; charset=utf-8"
}