package com.apollographql.apollo3

import com.apollographql.apollo3.api.Executable
import com.apollographql.apollo3.api.MergedField
import com.apollographql.apollo3.cache.normalized.CacheKey
import com.apollographql.apollo3.cache.normalized.CacheKeyResolver

class IdFieldCacheKeyResolver : CacheKeyResolver() {
  override fun fromFieldRecordSet(field: MergedField, variables: Executable.Variables, recordSet: Map<String, Any?>): CacheKey {
    val id = recordSet["id"]
    return if (id != null) {
      formatCacheKey(id.toString())
    } else {
      formatCacheKey(null)
    }
  }

  override fun fromFieldArguments(field: MergedField, variables: Executable.Variables): CacheKey {
    val id = field.resolveArgument("id", variables)
    return if (id != null) {
      formatCacheKey(id.toString())
    } else {
      formatCacheKey(null)
    }
  }

  private fun formatCacheKey(id: String?): CacheKey {
    return if (id == null || id.isEmpty()) {
      CacheKey.NO_KEY
    } else {
      CacheKey(id)
    }
  }
}
