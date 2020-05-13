package com.apollographql.apollo.network

class GraphQLRequest(
    val operationName: String,
    val operationId: String,
    val document: String,
    val variables: String,
    val extensions: String = ""
)
