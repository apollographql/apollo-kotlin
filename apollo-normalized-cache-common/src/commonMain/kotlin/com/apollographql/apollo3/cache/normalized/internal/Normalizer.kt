package com.apollographql.apollo3.cache.normalized.internal

import com.apollographql.apollo3.api.CompiledSelection
import com.apollographql.apollo3.cache.normalized.Record

interface Normalizer {
  fun normalize(map: Map<String, Any?>, selections: List<CompiledSelection>): Map<String, Record>
}