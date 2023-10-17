package com.apollographql.apollo3.mockserver

import okio.ByteString

class MockRequest(
    val method: String,
    val path: String,
    val version: String,
    val headers: Map<String, String> = emptyMap(),
    val body: ByteString = ByteString.EMPTY,
)