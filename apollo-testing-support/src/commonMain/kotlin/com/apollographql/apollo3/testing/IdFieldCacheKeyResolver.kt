package com.apollographql.apollo3.testing

import com.apollographql.apollo3.api.CompiledField
import com.apollographql.apollo3.api.Executable
import com.apollographql.apollo3.cache.normalized.CacheKey
import com.apollographql.apollo3.cache.normalized.CacheKeyResolver


object IdFieldCacheKeyResolver : CacheKeyResolver() {
  override fun fromFieldRecordSet(field: CompiledField, variables: Executable.Variables, recordSet: Map<String, Any?>): CacheKey? {
    val id = recordSet["id"]?.toString()
    return id?.let { CacheKey(it) }
  }

  override fun fromFieldArguments(field: CompiledField, variables: Executable.Variables): CacheKey? {
    val id = field.resolveArgument("id", variables)?.toString()
    return id?.let { CacheKey(it) }
  }
}