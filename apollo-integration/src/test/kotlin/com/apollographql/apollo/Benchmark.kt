package com.apollographql.apollo

import com.apollographql.apollo.api.CustomScalarAdapters
import com.apollographql.apollo.api.ResponseField
import com.apollographql.apollo.api.internal.json.JsonReader
import com.apollographql.apollo.api.parse
import com.apollographql.apollo.cache.CacheHeaders
import com.apollographql.apollo.cache.normalized.CacheKey
import com.apollographql.apollo.cache.normalized.CacheKeyResolver
import com.apollographql.apollo.cache.normalized.Record
import com.apollographql.apollo.cache.normalized.RecordFieldJsonAdapter
import com.apollographql.apollo.cache.normalized.internal.ReadableStore
import com.apollographql.apollo.cache.normalized.internal.RealCacheKeyBuilder
import com.apollographql.apollo.cache.normalized.internal.ResponseNormalizer
import com.apollographql.apollo.cache.normalized.internal.normalize
import com.apollographql.apollo.cache.normalized.internal.readDataFromCache
import com.apollographql.apollo.cache.normalized.sql.SqlNormalizedCacheFactory
import com.apollographql.apollo.integration.benchmark.GetResponseQuery
import okio.Buffer
import org.junit.Test

val responseNormalizer = object : ResponseNormalizer<Map<String, Any>?>() {
  override fun cacheKeyBuilder() = RealCacheKeyBuilder()

  override fun resolveCacheKey(field: ResponseField, record: Map<String, Any>?) = CacheKey.NO_KEY
}

@Test
fun apolloReadCache() {
  val operation = GetResponseQuery()
  val cache = SqlNormalizedCacheFactory("jdbc:sqlite:").create(RecordFieldJsonAdapter())

  val bufferedSource = Buffer().writeUtf8(Utils.readFileToString(Utils::class.java, "/largesample.json"))

  val data1 = operation.parse(bufferedSource).data!!

  val records = operation.normalize(data1, CustomScalarAdapters.DEFAULT, responseNormalizer)
  cache.merge(records, CacheHeaders.NONE)

  val readableStore = object : ReadableStore {
    override fun read(key: String, cacheHeaders: CacheHeaders): Record? {
      return cache.loadRecord(key, cacheHeaders)
    }

    override fun read(keys: Collection<String>, cacheHeaders: CacheHeaders): Collection<Record> {
      return cache.loadRecords(keys, cacheHeaders)
    }

    override fun stream(key: String, cacheHeaders: CacheHeaders): JsonReader? {
      return cache.stream(key, cacheHeaders)
    }
  }
  val data2 = operation.readDataFromCache(CustomScalarAdapters.DEFAULT, readableStore, CacheKeyResolver.DEFAULT, CacheHeaders.NONE)
  println(data2)
}