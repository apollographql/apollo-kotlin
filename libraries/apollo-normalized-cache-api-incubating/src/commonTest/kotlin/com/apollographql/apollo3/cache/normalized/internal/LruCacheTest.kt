package com.apollographql.apollo3.cache.normalized.internal

import com.apollographql.apollo3.cache.normalized.api.internal.LruCache
import com.apollographql.apollo3.testing.internal.runTest
import kotlinx.coroutines.delay
import kotlin.test.Test
import kotlin.test.assertEquals

class LruCacheTest {
  @Test
  fun emptyCache() {
    val cache = LruCache<String, String>(10, -1) { _, _ -> 1 }

    assertEquals(0, cache.size())
    assertEquals(null, cache["key"])
    assertEquals(null, cache.remove("key"))
    assertEquals(mapOf(), cache.dump())

    cache.clear()
    assertEquals(0, cache.size())
  }

  @Test
  fun addNewItemsToCache() {
    val cache = LruCache<String, String>(10, -1) { _, _ -> 1 }

    val expectedEntries = mapOf(
        "key1" to "value1",
        "key2" to "value2",
        "key3" to "value3"
    )

    expectedEntries.forEach { (key, value) ->
      cache[key] = value
    }

    assertEquals(3, cache.size())
    assertEquals(expectedEntries, cache.dump())
  }

  @Test
  fun removeItemsFromCache() {
    val cache = LruCache<String, String>(10, -1) { _, _ -> 1 }

    mapOf(
        "key1" to "value1",
        "key2" to "value2",
        "key3" to "value3"
    ).forEach { (key, value) ->
      cache[key] = value
    }

    assertEquals(null, cache.remove("key"))
    assertEquals("value1", cache.remove("key1"))
    assertEquals("value3", cache.remove("key3"))

    assertEquals(1, cache.size())
    assertEquals(mapOf("key2" to "value2"), cache.dump())
  }

  @Test
  fun clearCache() {
    val cache = LruCache<String, String>(10, -1) { _, _ -> 1 }

    mapOf(
        "key1" to "value1",
        "key2" to "value2",
        "key3" to "value3"
    ).forEach { (key, value) ->
      cache[key] = value
    }

    cache.clear()

    assertEquals(0, cache.size())
    assertEquals(mapOf(), cache.dump())
  }

  @Test
  fun trimCache() {
    val cache = LruCache<String, String>(2, -1) { _, _ -> 1 }

    mapOf(
        "key1" to "value1",
        "key2" to "value2",
        "key3" to "value3"
    ).forEach { (key, value) ->
      cache[key] = value
    }

    assertEquals(2, cache.size())
    assertEquals(
        mapOf(
            "key2" to "value2",
            "key3" to "value3"
        ),
        cache.dump()
    )
  }

  @Test
  fun addItemToCacheWithCustomWeigher() {
    val cache = LruCache<String, String>(100, -1) { key, value ->
      key.length + value.length
    }

    mapOf(
        "key1" to "value1",
        "key2" to "value2",
        "key3" to "value3"
    ).forEach { (key, value) ->
      cache[key] = value
    }

    assertEquals(30, cache.size())
  }

  @Test
  fun removeItemFromCacheWithCustomWeigher() {
    val cache = LruCache<String, String>(100, -1) { key, value ->
      key.length + value.length
    }

    mapOf(
        "key1" to "value1",
        "key2" to "value2",
        "key3" to "value3"
    ).forEach { (key, value) ->
      cache[key] = value
    }

    cache.remove("key2")
    cache.remove("key3")

    assertEquals(10, cache.size())
  }

  @Test
  fun trimCacheWithCustomWeigher() {
    val cache = LruCache<String, String>(12, -1) { key, value ->
      key.length + value.length
    }

    mapOf(
        "key1" to "value1",
        "key2" to "value2",
        "key3" to "value3"
    ).forEach { (key, value) ->
      cache[key] = value
    }

    assertEquals(10, cache.size())
    assertEquals(
        mapOf(
            "key3" to "value3"
        ),
        cache.dump()
    )
  }

  @Test
  fun recentUsedItem() {
    val cache = LruCache<String, String>(10, -1) { _, _ -> 1 }

    mapOf(
        "key1" to "value1",
        "key2" to "value2",
        "key3" to "value3"
    ).forEach { (key, value) ->
      cache[key] = value
    }

    cache["key3"]
    cache["key2"]

    assertEquals(
        mapOf(
            "key2" to "value2",
            "key3" to "value3",
            "key1" to "value1",
        ),
        cache.dump()
    )
  }

  @Test
  fun expiration() = runTest {
    val cache = LruCache<String, String>(10, 100) { _, _ -> 1 }

    mapOf(
        "key1" to "value1",
        "key2" to "value2",
        "key3" to "value3"
    ).forEach { (key, value) ->
      cache[key] = value
    }

    assertEquals(
        mapOf(
            "key1" to "value1",
            "key2" to "value2",
            "key3" to "value3"
        ),
        cache.dump()
    )

    delay(200)
    cache["key4"] = "value4"

    assertEquals(
        mapOf(
            "key4" to "value4"
        ),
        cache.dump()
    )
  }
}
