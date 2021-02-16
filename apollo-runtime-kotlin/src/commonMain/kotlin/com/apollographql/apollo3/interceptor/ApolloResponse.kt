package com.apollographql.apollo3.interceptor

import com.apollographql.apollo3.api.ApolloExperimental
import com.apollographql.apollo3.api.ExecutionContext
import com.apollographql.apollo3.api.Operation
import com.apollographql.apollo3.api.Response
import com.benasher44.uuid.Uuid

@ApolloExperimental
data class ApolloResponse<D : Operation.Data>(
  val requestUuid: Uuid,
  val response: Response<D>,
  val executionContext: ExecutionContext
)
