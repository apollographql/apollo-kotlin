package test

import com.apollographql.apollo3.api.json.JsonReader
import com.apollographql.apollo3.api.json.MapJsonReader
import kotlin.test.Test
import kotlin.test.assertEquals

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
}