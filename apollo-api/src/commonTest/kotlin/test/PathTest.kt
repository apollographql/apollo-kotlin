package test

import com.apollographql.apollo3.annotations.ApolloInternal
import com.apollographql.apollo3.api.json.BufferedSourceJsonReader
import com.apollographql.apollo3.api.json.JsonReader
import com.apollographql.apollo3.api.json.JsonReader.Token.BEGIN_ARRAY
import com.apollographql.apollo3.api.json.JsonReader.Token.BEGIN_OBJECT
import com.apollographql.apollo3.api.json.JsonReader.Token.BOOLEAN
import com.apollographql.apollo3.api.json.JsonReader.Token.END_ARRAY
import com.apollographql.apollo3.api.json.JsonReader.Token.END_DOCUMENT
import com.apollographql.apollo3.api.json.JsonReader.Token.END_OBJECT
import com.apollographql.apollo3.api.json.JsonReader.Token.LONG
import com.apollographql.apollo3.api.json.JsonReader.Token.NAME
import com.apollographql.apollo3.api.json.JsonReader.Token.NULL
import com.apollographql.apollo3.api.json.JsonReader.Token.NUMBER
import com.apollographql.apollo3.api.json.JsonReader.Token.STRING
import com.apollographql.apollo3.api.json.MapJsonReader
import com.apollographql.apollo3.api.json.MapJsonReader.Companion.buffer
import com.apollographql.apollo3.api.json.readAny
import okio.Buffer
import okio.ByteString.Companion.encodeUtf8
import kotlin.test.Test
import kotlin.test.assertEquals

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
    while (true) {
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

        END_DOCUMENT -> break
      }
      tokensAndPaths.add(token)
      tokensAndPaths.add(jsonReader.getPath())
    }

    val expected = mutableListOf<Any>(
        BEGIN_OBJECT,
        "",
        NAME,
        "menu",
        BEGIN_OBJECT,
        "menu.",
        NAME,
        "menu.id",
        STRING,
        "menu.id",
        NAME,
        "menu.value",
        BOOLEAN,
        "menu.value",
        NAME,
        "menu.popup",
        BEGIN_OBJECT,
        "menu.popup.",
        NAME,
        "menu.popup.menuitem",
        BEGIN_ARRAY,
        "menu.popup.menuitem.0",
        BEGIN_OBJECT,
        "menu.popup.menuitem.0.",
        NAME,
        "menu.popup.menuitem.0.value",
        STRING,
        "menu.popup.menuitem.0.value",
        NAME,
        "menu.popup.menuitem.0.onclick",
        STRING,
        "menu.popup.menuitem.0.onclick",
        NAME,
        "menu.popup.menuitem.0.template",
        BEGIN_OBJECT,
        "menu.popup.menuitem.0.template.",
        NAME,
        "menu.popup.menuitem.0.template.id",
        STRING,
        "menu.popup.menuitem.0.template.id",
        END_OBJECT,
        "menu.popup.menuitem.0.template",
        END_OBJECT,
        "menu.popup.menuitem.1",
        BEGIN_OBJECT,
        "menu.popup.menuitem.1.",
        NAME,
        "menu.popup.menuitem.1.value",
        STRING,
        "menu.popup.menuitem.1.value",
        NAME,
        "menu.popup.menuitem.1.onclick",
        STRING,
        "menu.popup.menuitem.1.onclick",
        NAME,
        "menu.popup.menuitem.1.template",
        BEGIN_OBJECT,
        "menu.popup.menuitem.1.template.",
        NAME,
        "menu.popup.menuitem.1.template.id",
        STRING,
        "menu.popup.menuitem.1.template.id",
        END_OBJECT,
        "menu.popup.menuitem.1.template",
        END_OBJECT,
        "menu.popup.menuitem.2",
        END_ARRAY,
        "menu.popup.menuitem",
        NAME,
        "menu.popup.booleanList",
        BEGIN_ARRAY,
        "menu.popup.booleanList.0",
        BOOLEAN,
        "menu.popup.booleanList.1",
        BOOLEAN,
        "menu.popup.booleanList.2",
        BOOLEAN,
        "menu.popup.booleanList.3",
        END_ARRAY,
        "menu.popup.booleanList",
        NAME,
        "menu.popup.arrayList",
        BEGIN_ARRAY,
        "menu.popup.arrayList.0",
        BEGIN_ARRAY,
        "menu.popup.arrayList.0.0",
        STRING,
        "menu.popup.arrayList.0.1",
        STRING,
        "menu.popup.arrayList.0.2",
        STRING,
        "menu.popup.arrayList.0.3",
        END_ARRAY,
        "menu.popup.arrayList.1",
        BEGIN_ARRAY,
        "menu.popup.arrayList.1.0",
        STRING,
        "menu.popup.arrayList.1.1",
        STRING,
        "menu.popup.arrayList.1.2",
        STRING,
        "menu.popup.arrayList.1.3",
        END_ARRAY,
        "menu.popup.arrayList.2",
        END_ARRAY,
        "menu.popup.arrayList",
        END_OBJECT,
        "menu.popup",
        END_OBJECT,
        "menu",
        END_OBJECT,
        "",
    )
    assertEquals(expected, tokensAndPaths)
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
    assertEquals("menu.popup", mapJsonReader.getPath())
    mapJsonReader.beginObject() // menu.popup.
    assertEquals("menu.popup.", mapJsonReader.getPath())
    mapJsonReader.nextName() // menu.popup.menuitem
    assertEquals("menu.popup.menuitem", mapJsonReader.getPath())
  }

  @Test
  @OptIn(ApolloInternal::class)
  fun skipPathRoot() {
    @Suppress("UNCHECKED_CAST") val root = BufferedSourceJsonReader(Buffer().write(jsonStr.encodeUtf8())).readAny() as Map<String, Any?>
    val mapJsonReader = MapJsonReader(root, skipPathRoot = true)
    mapJsonReader.beginObject() // root
    mapJsonReader.nextName() // menu
    mapJsonReader.beginObject() // menu.
    mapJsonReader.nextName() // menu.id
    assertEquals("id", mapJsonReader.getPath())
  }
}
