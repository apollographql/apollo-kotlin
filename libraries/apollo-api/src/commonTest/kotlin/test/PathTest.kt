package test

import com.apollographql.apollo.api.json.BufferedSourceJsonReader
import com.apollographql.apollo.api.json.JsonReader
import com.apollographql.apollo.api.json.JsonReader.Token.BEGIN_ARRAY
import com.apollographql.apollo.api.json.JsonReader.Token.BEGIN_OBJECT
import com.apollographql.apollo.api.json.JsonReader.Token.BOOLEAN
import com.apollographql.apollo.api.json.JsonReader.Token.END_ARRAY
import com.apollographql.apollo.api.json.JsonReader.Token.END_DOCUMENT
import com.apollographql.apollo.api.json.JsonReader.Token.END_OBJECT
import com.apollographql.apollo.api.json.JsonReader.Token.LONG
import com.apollographql.apollo.api.json.JsonReader.Token.NAME
import com.apollographql.apollo.api.json.JsonReader.Token.NULL
import com.apollographql.apollo.api.json.JsonReader.Token.NUMBER
import com.apollographql.apollo.api.json.JsonReader.Token.STRING
import com.apollographql.apollo.api.json.JsonReader.Token.ANY
import com.apollographql.apollo.api.json.MapJsonReader.Companion.buffer
import okio.Buffer
import okio.ByteString.Companion.encodeUtf8
import kotlin.test.Test
import kotlin.test.assertContentEquals

class PathTest {
  private val jsonStr = """
      {
        "menu": {
          "id": "file",
          "value": true,
          "popup": {
            "menuitem": [
              {
                "value": "New",
                "onclick": "CreateNewDoc()",
                "template": {
                  "id": "a000"
                }
              },
              {
                "value": "Open",
                "onclick": "OpenDoc()",
                "template": {
                  "id": "a001"
                }
              }
            ],
            "booleanList": [
              true,
              false,
              true
            ],
            "arrayList": [
              [
                "a",
                "b",
                "c"
              ],
              [
                "d",
                "e",
                "f"
              ]
            ]
          }
        }
      }
      """

  private fun testPath(jsonReader: JsonReader) {
    val tokensAndPaths = mutableListOf<Any>()
    loop@ while (true) {
      val token = jsonReader.peek()
      when (token) {
        BEGIN_ARRAY -> jsonReader.beginArray()
        END_ARRAY -> jsonReader.endArray()
        BEGIN_OBJECT -> jsonReader.beginObject()
        END_OBJECT -> jsonReader.endObject()
        NAME -> jsonReader.nextName()
        STRING -> jsonReader.nextString()
        NUMBER -> jsonReader.nextNumber()
        LONG -> jsonReader.nextLong()
        BOOLEAN -> jsonReader.nextBoolean()
        NULL -> jsonReader.nextNull()
        ANY -> jsonReader.skipValue()

        END_DOCUMENT -> break@loop
      }
      tokensAndPaths.add(token)
      tokensAndPaths.add(jsonReader.getPath())
    }

    val expected = mutableListOf(
        BEGIN_OBJECT,
        emptyList<Any>(),
        NAME,
        listOf("menu"),
        BEGIN_OBJECT,
        listOf("menu"),
        NAME,
        listOf("menu", "id"),
        STRING,
        listOf("menu", "id"),
        NAME,
        listOf("menu", "value"),
        BOOLEAN,
        listOf("menu", "value"),
        NAME,
        listOf("menu", "popup"),
        BEGIN_OBJECT,
        listOf("menu", "popup"),
        NAME,
        listOf("menu", "popup", "menuitem"),
        BEGIN_ARRAY,
        listOf("menu", "popup", "menuitem", 0),
        BEGIN_OBJECT,
        listOf("menu", "popup", "menuitem", 0),
        NAME,
        listOf("menu", "popup", "menuitem", 0, "value"),
        STRING,
        listOf("menu", "popup", "menuitem", 0, "value"),
        NAME,
        listOf("menu", "popup", "menuitem", 0, "onclick"),
        STRING,
        listOf("menu", "popup", "menuitem", 0, "onclick"),
        NAME,
        listOf("menu", "popup", "menuitem", 0, "template"),
        BEGIN_OBJECT,
        listOf("menu", "popup", "menuitem", 0, "template"),
        NAME,
        listOf("menu", "popup", "menuitem", 0, "template", "id"),
        STRING,
        listOf("menu", "popup", "menuitem", 0, "template", "id"),
        END_OBJECT,
        listOf("menu", "popup", "menuitem", 0, "template"),
        END_OBJECT,
        listOf("menu", "popup", "menuitem", 1),
        BEGIN_OBJECT,
        listOf("menu", "popup", "menuitem", 1),
        NAME,
        listOf("menu", "popup", "menuitem", 1, "value"),
        STRING,
        listOf("menu", "popup", "menuitem", 1, "value"),
        NAME,
        listOf("menu", "popup", "menuitem", 1, "onclick"),
        STRING,
        listOf("menu", "popup", "menuitem", 1, "onclick"),
        NAME,
        listOf("menu", "popup", "menuitem", 1, "template"),
        BEGIN_OBJECT,
        listOf("menu", "popup", "menuitem", 1, "template"),
        NAME,
        listOf("menu", "popup", "menuitem", 1, "template", "id"),
        STRING,
        listOf("menu", "popup", "menuitem", 1, "template", "id"),
        END_OBJECT,
        listOf("menu", "popup", "menuitem", 1, "template"),
        END_OBJECT,
        listOf("menu", "popup", "menuitem", 2),
        END_ARRAY,
        listOf("menu", "popup", "menuitem"),
        NAME,
        listOf("menu", "popup", "booleanList"),
        BEGIN_ARRAY,
        listOf("menu", "popup", "booleanList", 0),
        BOOLEAN,
        listOf("menu", "popup", "booleanList", 1),
        BOOLEAN,
        listOf("menu", "popup", "booleanList", 2),
        BOOLEAN,
        listOf("menu", "popup", "booleanList", 3),
        END_ARRAY,
        listOf("menu", "popup", "booleanList"),
        NAME,
        listOf("menu", "popup", "arrayList"),
        BEGIN_ARRAY,
        listOf("menu", "popup", "arrayList", 0),
        BEGIN_ARRAY,
        listOf("menu", "popup", "arrayList", 0, 0),
        STRING,
        listOf("menu", "popup", "arrayList", 0, 1),
        STRING,
        listOf("menu", "popup", "arrayList", 0, 2),
        STRING,
        listOf("menu", "popup", "arrayList", 0, 3),
        END_ARRAY,
        listOf("menu", "popup", "arrayList", 1),
        BEGIN_ARRAY,
        listOf("menu", "popup", "arrayList", 1, 0),
        STRING,
        listOf("menu", "popup", "arrayList", 1, 1),
        STRING,
        listOf("menu", "popup", "arrayList", 1, 2),
        STRING,
        listOf("menu", "popup", "arrayList", 1, 3),
        END_ARRAY,
        listOf("menu", "popup", "arrayList", 2),
        END_ARRAY,
        listOf("menu", "popup", "arrayList"),
        END_OBJECT,
        listOf("menu", "popup"),
        END_OBJECT,
        listOf("menu"),
        END_OBJECT,
        emptyList<Any>()
    )
    assertContentEquals(expected, tokensAndPaths)
  }

  @Test
  fun bufferedSourceJsonReaderPath() {
    val bufferedSourceJsonReader = BufferedSourceJsonReader(Buffer().write(jsonStr.encodeUtf8()))
    testPath(bufferedSourceJsonReader)
  }

  @Test
  fun mapJsonReaderPath() {
    val mapJsonReader = BufferedSourceJsonReader(Buffer().write(jsonStr.encodeUtf8())).buffer()
    testPath(mapJsonReader)
  }

  @Test
  fun pathWithBuffer() {
    val bufferedSourceJsonReader = BufferedSourceJsonReader(Buffer().write(jsonStr.encodeUtf8()))
    bufferedSourceJsonReader.beginObject() // root
    bufferedSourceJsonReader.nextName() // menu
    bufferedSourceJsonReader.beginObject() // menu.
    bufferedSourceJsonReader.nextName() // menu.id
    bufferedSourceJsonReader.nextString() // menu.id
    bufferedSourceJsonReader.nextName() // menu.value
    bufferedSourceJsonReader.nextBoolean() // menu.value
    bufferedSourceJsonReader.nextName() // menu.popup
    val mapJsonReader = bufferedSourceJsonReader.buffer()
    assertContentEquals(listOf("menu", "popup"), mapJsonReader.getPath())
    mapJsonReader.beginObject() // menu.popup.
    assertContentEquals(listOf("menu", "popup"), mapJsonReader.getPath())
    mapJsonReader.nextName() // menu.popup.menuitem
    assertContentEquals(listOf("menu", "popup", "menuitem"), mapJsonReader.getPath())
  }
}
