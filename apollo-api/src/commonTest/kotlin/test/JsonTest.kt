package test

import com.apollographql.apollo3.api.AnyAdapter
import com.apollographql.apollo3.api.CustomScalarAdapters
import com.apollographql.apollo3.api.LongAdapter
import com.apollographql.apollo3.api.json.internal.MapJsonWriter
import com.apollographql.apollo3.api.internal.json.buildJsonString
import kotlin.test.Test
import kotlin.test.assertEquals

class JsonTest {
  @Test
  fun longAdapterWritesNumbers() {
    val json = buildJsonString {
      LongAdapter.toJson(this, CustomScalarAdapters.Empty, Long.MAX_VALUE)
    }
    assertEquals("9223372036854775807", json)
  }

  @Test
  fun longAdapterInBufferedObjectWritesNumbers() {
    val mapWriter = MapJsonWriter()
    mapWriter.value(Long.MAX_VALUE)

    val json = buildJsonString {
      AnyAdapter.toJson(this, mapWriter.root()!!)
    }
    assertEquals("9223372036854775807", json)
  }
}