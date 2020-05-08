package com.apollographql.apollo.network

class NetworkRequest(
    val operationName: String,
    val document: String,
    val variables: String,
    val extensions: String = ""
)
