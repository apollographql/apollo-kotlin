package com.apollographql.ijplugin.normalizedcache.provider

import com.apollographql.apollo.api.json.JsonNumber
import com.apollographql.apollo.cache.normalized.api.CacheKey
import com.apollographql.ijplugin.apollodebugserver.GetNormalizedCacheQuery
import com.apollographql.ijplugin.normalizedcache.NormalizedCache
import com.apollographql.ijplugin.normalizedcache.NormalizedCache.Field
import com.apollographql.ijplugin.normalizedcache.NormalizedCache.FieldValue
import com.apollographql.ijplugin.normalizedcache.NormalizedCache.FieldValue.BooleanValue
import com.apollographql.ijplugin.normalizedcache.NormalizedCache.FieldValue.CompositeValue
import com.apollographql.ijplugin.normalizedcache.NormalizedCache.FieldValue.ListValue
import com.apollographql.ijplugin.normalizedcache.NormalizedCache.FieldValue.Null
import com.apollographql.ijplugin.normalizedcache.NormalizedCache.FieldValue.NumberValue
import com.apollographql.ijplugin.normalizedcache.NormalizedCache.FieldValue.Reference
import com.apollographql.ijplugin.normalizedcache.NormalizedCache.FieldValue.StringValue

class ApolloDebugNormalizedCacheProvider : NormalizedCacheProvider<GetNormalizedCacheQuery.NormalizedCache> {
  override fun provide(parameters: GetNormalizedCacheQuery.NormalizedCache): Result<NormalizedCache> {
    return runCatching {
      NormalizedCache(
          parameters.records.map { record ->
            NormalizedCache.Record(
                key = record.key,
                fields = record.fields.toFields(),
                sizeInBytes = record.sizeInBytes,
            )
          }
      )
    }
  }
}

@Suppress("UNCHECKED_CAST")
private fun Any.toFields(): List<Field> {
  this as Map<String, Any?>
  return map { (name, value) ->
    Field(
        name,
        value.toFieldValue()
    )
  }
}

private fun Any?.toFieldValue(): FieldValue {
  return when (this) {
    null -> Null
    is String -> if (CacheKey.canDeserialize(this)) {
      Reference(CacheKey.deserialize(this).key)
    } else {
      StringValue(this)
    }

    is Number -> NumberValue(this.toString())
    is JsonNumber -> NumberValue(this.value)
    is Boolean -> BooleanValue(this)
    is List<*> -> ListValue(map { it.toFieldValue() })
    is Map<*, *> -> CompositeValue(map { Field(it.key as String, it.value.toFieldValue()) })
    else -> error("Unsupported type ${this::class}")
  }
}
