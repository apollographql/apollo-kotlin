package com.apollographql.ijplugin.normalizedcache.provider

import com.apollographql.apollo3.cache.normalized.api.CacheKey
import com.apollographql.apollo3.debug.GetNormalizedCacheQuery
import com.apollographql.ijplugin.normalizedcache.NormalizedCache
import com.apollographql.ijplugin.normalizedcache.NormalizedCache.FieldValue

class ApolloDebugNormalizedCacheProvider : NormalizedCacheProvider<GetNormalizedCacheQuery.NormalizedCache> {
  override fun provide(parameters: GetNormalizedCacheQuery.NormalizedCache): Result<NormalizedCache> {
    return runCatching {
      NormalizedCache(
          parameters.records.map { record ->
            NormalizedCache.Record(
                key = record.key,
                fields = record.record.toField(),
                size = record.size
            )
          }
      )
    }
  }
}

@Suppress("UNCHECKED_CAST")
private fun Any.toField(): List<NormalizedCache.Field> {
  this as Map<String, Any?>
  return map { (name, value) ->
    NormalizedCache.Field(
        name,
        value.toFieldValue()
    )
  }
}

private fun Any?.toFieldValue(): FieldValue {
  return when (this) {
    null -> FieldValue.Null
    is String -> if (CacheKey.canDeserialize(this)) {
      FieldValue.Reference(CacheKey.deserialize(this).key)
    } else {
      FieldValue.StringValue(this)
    }

    is Number -> FieldValue.NumberValue(this)
    is Boolean -> FieldValue.BooleanValue(this)
    is List<*> -> FieldValue.ListValue(map { it.toFieldValue() })
    is Map<*, *> -> FieldValue.CompositeValue(map { NormalizedCache.Field(it.key as String, it.value.toFieldValue()) })
    else -> error("Unsupported type ${this::class}")
  }
}
