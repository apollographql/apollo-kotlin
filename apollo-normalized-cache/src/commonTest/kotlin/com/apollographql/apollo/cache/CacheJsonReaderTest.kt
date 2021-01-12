package com.apollographql.apollo.cache

import com.apollographql.apollo.api.internal.json.BufferedSourceJsonReader
import com.apollographql.apollo.api.internal.json.JsonReader
import com.apollographql.apollo.cache.normalized.Record
import com.apollographql.apollo.cache.normalized.internal.CacheJsonReader
import com.apollographql.apollo.cache.normalized.internal.ReadableStore
import okio.Buffer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.fail

class CacheJsonReaderTest {
  val cache = object : ReadableStore {
    override fun stream(key: String, cacheHeaders: CacheHeaders): JsonReader? {
      return when (key) {
        "root" -> BufferedSourceJsonReader(Buffer().writeUtf8("""
          {
            "hero": "ApolloCacheReference{1}"
          }
        """.trimIndent()))
        "1" -> BufferedSourceJsonReader(Buffer().writeUtf8("""
          {
            "name": "Luke"
          }
        """.trimIndent()))
        else -> error("")
      }
    }

    override fun read(key: String, cacheHeaders: CacheHeaders): Record? {
      return null
    }

    override fun read(keys: Collection<String>, cacheHeaders: CacheHeaders): Collection<Record> {
      return emptySet()
    }
  }

  @Test
  fun test() {
    val jsonReader = CacheJsonReader(rootKey = "root", cache, CacheHeaders.NONE)
    jsonReader.beginObject()
    while(jsonReader.hasNext()) {
      when (jsonReader.nextName()) {
        "hero" -> {
          jsonReader.beginObject()
          while (jsonReader.hasNext()) {
            when(jsonReader.nextName()) {
              "name" -> assertEquals("Luke", jsonReader.nextString())
            }
          }
          jsonReader.endObject()
        }
        else -> fail()
      }
    }
    jsonReader.endObject()
  }
}