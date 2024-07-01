package com.apollographql.apollo.cache.normalized.internal

import com.apollographql.apollo.cache.normalized.api.internal.LruCache
import kotlin.test.Test
import kotlin.test.assertEquals

class LruCacheTest {
  @Test
  fun emptyCache() {
    val cache = LruCache<String, String?>(10)

    assertEquals(0, cache.size())
    assertEquals(null, cache["key"])
    assertEquals(null, cache.remove("key"))
    assertEquals(mapOf(), cache.dump())

    cache.clear()
    assertEquals(0, cache.size())
  }

  @Test
  fun addNewItemsToCache() {
    val cache = LruCache<String, String?>(10)

    val expectedEntries = mapOf(
        "key1" to "value1",
        "key2" to "value2",
        "key3" to null
    )

    expectedEntries.forEach { (key, value) ->
      cache[key] = value
    }

    assertEquals(3, cache.size())
    assertEquals(expectedEntries, cache.dump())
  }

  @Test
  fun removeItemsFromCache() {
    val cache = LruCache<String, String?>(10)

    mapOf(
        "key1" to "value1",
        "key2" to "value2",
        "key3" to null
    ).forEach { (key, value) ->
      cache[key] = value
    }

    assertEquals(null, cache.remove("key"))
    assertEquals("value1", cache.remove("key1"))
    assertEquals(null, cache.remove("key3"))

    assertEquals(1, cache.size())
    assertEquals(mapOf("key2" to "value2"), cache.dump())
  }

  @Test
  fun clearCache() {
    val cache = LruCache<String, String?>(10)

    mapOf(
        "key1" to "value1",
        "key2" to "value2",
        "key3" to null
    ).forEach { (key, value) ->
      cache[key] = value
    }

    cache.clear()

    assertEquals(0, cache.size())
    assertEquals(mapOf(), cache.dump())
  }

  @Test
  fun trimCache() {
    val cache = LruCache<String, String?>(2)

    mapOf(
        "key1" to "value1",
        "key2" to "value2",
        "key3" to null
    ).forEach { (key, value) ->
      cache[key] = value
    }

    assertEquals(2, cache.size())
    assertEquals(
        mapOf(
            "key2" to "value2",
            "key3" to null
        ),
        cache.dump()
    )
  }

  @Test
  fun addItemToCacheWithCustomWeigher() {
    val cache = LruCache<String, String?>(100) { key, value ->
      key.length + (value?.length ?: 0)
    }

    mapOf(
        "key1" to "value1",
        "key2" to "value2",
        "key3" to null
    ).forEach { (key, value) ->
      cache[key] = value
    }

    assertEquals(24, cache.size())
  }

  @Test
  fun removeItemFromCacheWithCustomWeigher() {
    val cache = LruCache<String, String?>(100) { key, value ->
      key.length + (value?.length ?: 0)
    }

    mapOf(
        "key1" to "value1",
        "key2" to "value2",
        "key3" to null
    ).forEach { (key, value) ->
      cache[key] = value
    }

    cache.remove("key2")
    cache.remove("key3")

    assertEquals(10, cache.size())
  }

  @Test
  fun trimCacheWithCustomWeigher() {
    val cache = LruCache<String, String?>(12) { key, value ->
      key.length + (value?.length ?: 0)
    }

    mapOf(
        "key1" to "value1",
        "key2" to "value2",
        "key3" to null
    ).forEach { (key, value) ->
      cache[key] = value
    }

    assertEquals(4, cache.size())
    assertEquals(
        mapOf(
            "key3" to null
        ),
        cache.dump()
    )
  }

  @Test
  fun recentUsedItem() {
    val cache = LruCache<String, String?>(10)

    mapOf(
        "key1" to "value1",
        "key2" to "value2",
        "key3" to null
    ).forEach { (key, value) ->
      cache[key] = value
    }

    cache["key3"]
    cache["key2"]

    assertEquals(
        mapOf(
            "key2" to "value2",
            "key3" to null,
            "key1" to "value1",
        ),
        cache.dump()
    )
  }
}
