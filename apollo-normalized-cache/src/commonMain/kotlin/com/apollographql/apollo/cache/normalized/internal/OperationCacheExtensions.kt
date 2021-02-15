package com.apollographql.apollo.cache.normalized.internal

import com.apollographql.apollo.api.ResponseAdapterCache
import com.apollographql.apollo.api.Fragment
import com.apollographql.apollo.api.Operation
import com.apollographql.apollo.cache.normalized.Record
import com.apollographql.apollo.api.ResponseField
import com.apollographql.apollo.api.internal.MapJsonReader
import com.apollographql.apollo.api.internal.ResponseAdapter
import com.apollographql.apollo.cache.CacheHeaders
import com.apollographql.apollo.cache.normalized.CacheKey
import com.apollographql.apollo.cache.normalized.CacheKeyResolver

fun <D : Operation.Data> Operation<D>.normalize(
    data: D,
    responseAdapterCache: ResponseAdapterCache,
    cacheKeyResolver: CacheKeyResolver
) = normalizeInternal(data, cacheKeyResolver, CacheKeyResolver.rootKey().key, adapter(responseAdapterCache), variables(), responseFields())

fun <D : Fragment.Data> Fragment<D>.normalize(
    data: D,
    responseAdapterCache: ResponseAdapterCache,
    cacheKeyResolver: CacheKeyResolver,
    rootKey: String
) = normalizeInternal(data, cacheKeyResolver, rootKey, adapter(responseAdapterCache), variables(), responseFields())

private fun <D> normalizeInternal(
    data: D,
    cacheKeyResolver: CacheKeyResolver,
    rootKey: String,
    adapter: ResponseAdapter<D>,
    variables: Operation.Variables,
    fieldSets: List<ResponseField.FieldSet>
): Map<String, Record>  {
  val writer = MapJsonWriter()
  adapter.toResponse(writer, data)
  return Normalizer(variables) { responseField, fields ->
    cacheKeyResolver.fromFieldRecordSet(responseField, fields).let { if (it == CacheKey.NO_KEY) null else it.key}
  }.normalize(writer.root() as Map<String, Any?>, null, rootKey, fieldSets)
}
enum class ReadMode {
  /**
   * Depth-first traversal. Resolve CacheReferences as they are encountered
   */
  SEQUENTIAL,

  /**
   * Breadth-first traversal. Batches CacheReferences at a certain depth and resolve them all at once. This is useful for SQLite
   */
  BATCH,
}

fun <D : Operation.Data> Operation<D>.readDataFromCache(
    responseAdapterCache: ResponseAdapterCache,
    readableStore: ReadableStore,
    cacheKeyResolver: CacheKeyResolver,
    cacheHeaders: CacheHeaders,
    mode: ReadMode = ReadMode.SEQUENTIAL,
) = readInternal(
    readableStore = readableStore,
    cacheKeyResolver = cacheKeyResolver,
    cacheHeaders = cacheHeaders,
    variables = variables(),
    adapter = adapter(responseAdapterCache),
    mode = mode,
    cacheKey = CacheKeyResolver.rootKey(),
    fieldSets = responseFields()
)

fun <D : Fragment.Data> Fragment<D>.readDataFromCache(
    cacheKey: CacheKey,
    responseAdapterCache: ResponseAdapterCache,
    readableStore: ReadableStore,
    cacheKeyResolver: CacheKeyResolver,
    cacheHeaders: CacheHeaders,
    mode: ReadMode = ReadMode.SEQUENTIAL
) = readInternal(
    cacheKey = cacheKey,
    readableStore = readableStore,
    cacheKeyResolver = cacheKeyResolver,
    cacheHeaders = cacheHeaders,
    variables = variables(),
    adapter = adapter(responseAdapterCache),
    mode = mode,
    fieldSets = responseFields()
)


private fun <D> readInternal(
    cacheKey: CacheKey,
    readableStore: ReadableStore,
    cacheKeyResolver: CacheKeyResolver,
    cacheHeaders: CacheHeaders,
    variables: Operation.Variables,
    adapter: ResponseAdapter<D>,
    mode: ReadMode = ReadMode.SEQUENTIAL,
    fieldSets: List<ResponseField.FieldSet>,
): D? = try {
    val map = if (mode == ReadMode.BATCH) {
      CacheBatchReader(
          readableStore = readableStore,
          cacheHeaders = cacheHeaders,
          cacheKeyResolver = cacheKeyResolver,
          variables = variables,
          rootKey = cacheKey.key,
          rootFieldSets = fieldSets
      ).toMap()
    } else {
      CacheSequentialReader(
          readableStore = readableStore,
          cacheHeaders = cacheHeaders,
          cacheKeyResolver = cacheKeyResolver,
          variables = variables,
          rootKey = cacheKey.key,
          rootFieldSets = fieldSets
      ).toMap()
    }

    val reader = MapJsonReader(
        root = map,
    )
    adapter.fromResponse(reader)
  } catch (e: Exception) {
    null
  }


fun Collection<Record>?.dependentKeys(): Set<String> {
  return this?.flatMap {
    it.keys() + it.key
  }?.toSet() ?: emptySet()
}
