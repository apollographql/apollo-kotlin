package com.apollographql.apollo3.cache.normalized.internal

import com.apollographql.apollo3.api.CompiledField
import com.apollographql.apollo3.api.Executable
import com.apollographql.apollo3.api.MergedField

interface CacheKeyBuilder {
  fun build(field: CompiledField, variables: Executable.Variables): String
}
