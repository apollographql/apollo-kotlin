package com.apollographql.apollo3.cache.normalized.internal

import com.apollographql.apollo3.api.Operation
import com.apollographql.apollo3.api.ResponseField

interface CacheKeyBuilder {
  fun build(field: ResponseField, variables: Operation.Variables): String
}
