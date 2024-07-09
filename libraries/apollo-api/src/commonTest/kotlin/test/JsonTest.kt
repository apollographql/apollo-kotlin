package test

import com.apollographql.apollo.api.AnyAdapter
import com.apollographql.apollo.api.CustomScalarAdapters
import com.apollographql.apollo.api.LongAdapter
import com.apollographql.apollo.api.json.MapJsonReader
import com.apollographql.apollo.api.json.MapJsonWriter
import com.apollographql.apollo.api.json.buildJsonString
import com.apollographql.apollo.api.json.jsonReader
import com.apollographql.apollo.api.json.readAny
import okio.Buffer
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
      AnyAdapter.toJson(this, CustomScalarAdapters.Empty, mapWriter.root()!!)
    }
    assertEquals("9223372036854775807", json)
  }

  @Test
  fun canReadAndWriteVeryDeeplyNestedJsonSource() {
    val json = buildJsonString {
      val nesting = 1025
      repeat(nesting) {
        beginObject()
        name("child")
      }
      value("yooooo")
      repeat(nesting) {
        endObject()
      }
    }

    Buffer().writeUtf8(json).jsonReader().readAny()
  }

  @Test
  fun canReadVeryDeeplyNestedJsonMap() {
    val root = mutableMapOf<String, Any>()
    var map = root
    val nesting = 1025

    repeat(nesting) {
      val newMap = mutableMapOf<String, Any>()
      map.put("child", newMap)
      map = newMap
    }

    MapJsonReader(root).readAny()
  }
}
