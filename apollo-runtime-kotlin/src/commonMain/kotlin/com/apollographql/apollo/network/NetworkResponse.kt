package com.apollographql.apollo.network

import com.apollographql.apollo.api.ExecutionContext
import okio.BufferedSource

class NetworkResponse(
    val body: BufferedSource,
    val executionContext: ExecutionContext
)
