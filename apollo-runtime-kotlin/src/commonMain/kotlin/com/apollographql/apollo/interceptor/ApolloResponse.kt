package com.apollographql.apollo.interceptor

import com.apollographql.apollo.api.ApolloExperimental
import com.apollographql.apollo.api.ExecutionContext
import com.apollographql.apollo.api.Operation
import com.apollographql.apollo.api.Response
import com.benasher44.uuid.Uuid

@ApolloExperimental
data class ApolloResponse<D : Operation.Data>(
  val requestUuid: Uuid,
  val response: Response<D>,
  val executionContext: ExecutionContext
)
