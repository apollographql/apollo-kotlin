package com.apollographql.apollo3.api.internal

import com.apollographql.apollo3.api.CustomScalarAdapters
import com.apollographql.apollo3.api.HasExecutionContext

val HasExecutionContext.customScalarAdapters: CustomScalarAdapters
  get() = executionContext[CustomScalarAdapters]!!