package com.apollographql.apollo3.cache.normalized.internal

import com.apollographql.apollo3.api.CompiledField
import com.apollographql.apollo3.api.Executable

interface CacheKeyBuilder {
  fun build(field: CompiledField, variables: Executable.Variables): String
}
