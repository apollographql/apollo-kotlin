package test

import com.apollographql.apollo3.api.json.MapJsonReader
import kotlin.test.Test
import kotlin.test.assertEquals

class NormalizedCacheCoMapJsonReaderTest {
  @Test
  fun canReadMap() {
    val map = mapOf(
        "hero" to mapOf(
            "name" to "Luke",
            "appearsIn" to "Episode1"
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
                appearsIn = jsonReader.nextString()
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

    assertEquals(name, "Luke")
    assertEquals(appearsIn, "Episode1")
  }
}