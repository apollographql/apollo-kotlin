package com.apollographql.apollo.cache.normalized.internal

import com.apollographql.apollo.api.Operation
import com.apollographql.apollo.api.ResponseField

interface CacheKeyBuilder {
  fun build(field: ResponseField, variables: Operation.Variables): String
}
