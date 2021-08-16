package com.apollographql.apollo3.api.internal

import com.apollographql.apollo3.api.CustomScalarAdapters
import com.apollographql.apollo3.api.ExecutionParameters

val <T> ExecutionParameters<T>.customScalarAdapters: CustomScalarAdapters where T: ExecutionParameters<T>
  get() = executionContext[CustomScalarAdapters]!!