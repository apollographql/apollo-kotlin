package com.apollographql.apollo.network

import com.benasher44.uuid.uuid4

class GraphQLRequest(
    val operationName: String,
    val operationId: String,
    val document: String,
    val variables: String,
    val extensions: String = ""
) {
  val uuid = uuid4()
}
