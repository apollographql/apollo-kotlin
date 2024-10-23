package test

import com.apollographql.apollo.api.json.JsonNumber
import com.apollographql.apollo.api.json.JsonReader
import com.apollographql.apollo.api.json.MapJsonReader
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MapJsonReaderTest {
  @Test
  fun canReadMap() {
    val map = mapOf(
        "hero" to mapOf(
            "name" to "Luke",
            "appearsIn" to listOf("Episode1", "Episode2", "Episode3")
        )
    )

    val jsonReader = MapJsonReader(map)

    var name: String? = null
    var appearsIn: String? = null
    jsonReader.beginObject()
    while (jsonReader.hasNext()) {
      when (jsonReader.nextName()) {
        "hero" -> {
          jsonReader.beginObject()
          while (jsonReader.hasNext()) {
            when (jsonReader.nextName()) {
              "name" -> {
                name = jsonReader.nextString()
              }
              "appearsIn" -> {
                jsonReader.beginArray()
                while (jsonReader.hasNext()) {
                  appearsIn = jsonReader.nextString()
                }
                jsonReader.endArray()
              }
              else -> jsonReader.skipValue()
            }
          }
          jsonReader.endObject()
        }
        else -> jsonReader.skipValue()
      }
    }
    jsonReader.endObject()

    assertEquals(jsonReader.peek(), JsonReader.Token.END_DOCUMENT)
    assertEquals(name, "Luke")
    assertEquals(appearsIn, "Episode3")
  }

  @Test
  fun canRewindInMap() {
    val map = mapOf(
        "key1" to "value1",
        "key2" to "value2",
    )

    val jsonReader = MapJsonReader(map)

    jsonReader.beginObject()
    assertEquals("key1", jsonReader.nextName())
    assertEquals("value1", jsonReader.nextString())
    assertEquals("key2", jsonReader.nextName())
    assertEquals("value2", jsonReader.nextString())
    jsonReader.rewind()
    assertEquals("key1", jsonReader.nextName())
    assertEquals("value1", jsonReader.nextString())
    jsonReader.skipValue()
    jsonReader.endObject()

    assertEquals(jsonReader.peek(), JsonReader.Token.END_DOCUMENT)
  }

  @Test
  fun nextStringWorksOnNumbers() {
    val map = mapOf(
        "key0" to 0,
        "key1" to 1.0,
        "key2" to 2L,
        "key3" to JsonNumber("3")
    )

    MapJsonReader(map).apply {
      beginObject()
      assertEquals("key0", nextName())
      assertEquals("0", nextString())
      assertEquals("key1", nextName())
      assertTrue(nextString().startsWith("1")) // In JS, 1.0.toString() returns "1", others return "1.0"
      assertEquals("key2", nextName())
      assertEquals("2", nextString())
      assertEquals("key3", nextName())
      assertEquals("3", nextString())
      endObject()
    }
  }
}