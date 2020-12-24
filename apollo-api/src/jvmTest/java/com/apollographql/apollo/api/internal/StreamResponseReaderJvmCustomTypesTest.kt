package com.apollographql.apollo.api.internal

import com.apollographql.apollo.api.CustomScalarAdapter
import com.apollographql.apollo.api.JsonElement
import com.apollographql.apollo.api.EMPTY_OPERATION
import com.apollographql.apollo.api.ResponseField
import com.apollographql.apollo.api.ScalarType
import com.apollographql.apollo.api.ScalarTypeAdapters
import com.apollographql.apollo.api.internal.json.BufferedSourceJsonReader
import com.apollographql.apollo.api.internal.json.JsonReader
import com.apollographql.apollo.api.internal.json.JsonUtf8Writer
import com.apollographql.apollo.api.internal.json.JsonWriter
import com.apollographql.apollo.api.internal.json.Utils
import okio.Buffer
import org.junit.Assert
import org.junit.Assert.assertEquals
import org.junit.Test
import java.net.URL
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Date

class StreamResponseReaderJvmCustomTypesTest {
  private val noConditions: List<ResponseField.Condition> = emptyList()

  @Test
  fun readCustom() {
    val successField = ResponseField.forCustomScalar("successFieldResponseName", "successFieldName", null,
        false, DATE_CUSTOM_TYPE, noConditions)
    val classCastExceptionField = ResponseField.forCustomScalar("classCastExceptionField",
        "classCastExceptionField", null, false, DATE_CUSTOM_TYPE, noConditions)
    val recordSet: MutableMap<String, Any> = HashMap()
    recordSet["successFieldResponseName"] = "2017-04-16"
    recordSet["successFieldName"] = "2018-04-16"
    recordSet["classCastExceptionField"] = 0
    val responseReader = responseReader(recordSet)
    assertEquals(DATE_TIME_FORMAT.parse("2017-04-16"), responseReader.readCustomScalar<Date>(successField))
    try {
      responseReader.readCustomScalar<Any>(classCastExceptionField)
      Assert.fail("expected ClassCastException")
    } catch (expected: ClassCastException) {
      // expected
    }
  }

  @Test
  fun readCustomList() {
    val successField = ResponseField.forList("successFieldResponseName", "successFieldName", null, false, noConditions)
    val recordSet: MutableMap<String, Any> = HashMap()
    recordSet["successFieldResponseName"] = listOf("2017-04-16", "2017-04-17", "2017-04-18")
    recordSet["successFieldName"] = listOf("2017-04-19", "2017-04-20")
    recordSet["classCastExceptionField"] = "anything"
    val responseReader = responseReader(recordSet)
    assertEquals(
        listOf(DATE_TIME_FORMAT.parse("2017-04-16"), DATE_TIME_FORMAT.parse("2017-04-17"), DATE_TIME_FORMAT.parse("2017-04-18")),
        responseReader.readList(successField) { reader -> reader.readCustomScalar<Date>(DATE_CUSTOM_TYPE) }
    )
  }

  @Test
  fun optionalFieldsIOException() {
    val responseReader = responseReader(emptyMap())
    responseReader.readCustomScalar<Any>(ResponseField.forCustomScalar("CustomScalarField", "CustomScalarField", null, true, DATE_CUSTOM_TYPE,
        noConditions))
  }

  @Test
  fun mandatoryFieldsIOException() {
    val responseReader = responseReader(emptyMap())
    try {
      responseReader.readCustomScalar<Any>(ResponseField.forCustomScalar("CustomScalarField", "CustomScalarField", null, false, DATE_CUSTOM_TYPE,
          noConditions))
      Assert.fail("expected NullPointerException")
    } catch (expected: NullPointerException) {
      //expected
    }
  }

  companion object {
    private fun responseReader(recordSet: Map<String, Any>): StreamResponseReader {
      val customScalarAdapters: MutableMap<ScalarType, CustomScalarAdapter<*>> = HashMap()
      customScalarAdapters[DATE_CUSTOM_TYPE] = object : CustomScalarAdapter<Any?> {
        override fun decode(jsonElement: JsonElement): Any {
          return try {
            DATE_TIME_FORMAT.parse(value.value.toString())
          } catch (e: ParseException) {
            throw ClassCastException()
          }
        }

        override fun encode(value: Any?): JsonElement {
          throw UnsupportedOperationException()
        }
      }
      customScalarAdapters[URL_CUSTOM_TYPE] = object : CustomScalarAdapter<Any?> {
        override fun decode(jsonElement: JsonElement): Any {
          throw UnsupportedOperationException()
        }

        override fun encode(value: Any?): JsonElement {
          throw UnsupportedOperationException()
        }
      }
      customScalarAdapters[OBJECT_CUSTOM_TYPE] = object : CustomScalarAdapter<Any?> {
        override fun decode(jsonElement: JsonElement): Any {
          return value.value.toString()
        }

        override fun encode(value: Any?): JsonElement {
          throw UnsupportedOperationException()
        }
      }

      val jsonReader = BufferedSourceJsonReader(
          Buffer().apply {
            Utils.writeToJson(
                value = recordSet,
                jsonWriter = JsonUtf8Writer(this),
            )
            flush()
          }
      )
      return StreamResponseReader(
          jsonReader = jsonReader,
          variables = EMPTY_OPERATION.variables(),
          scalarTypeAdapters = ScalarTypeAdapters(customScalarAdapters),
      )
    }

    private val OBJECT_CUSTOM_TYPE: ScalarType = object : ScalarType {
      override fun typeName(): String {
        return String::class.java.name
      }

      override fun className(): String {
        return String::class.java.name
      }
    }
    private val DATE_CUSTOM_TYPE: ScalarType = object : ScalarType {
      override fun typeName(): String {
        return Date::class.java.name
      }

      override fun className(): String {
        return Date::class.java.name
      }
    }
    private val URL_CUSTOM_TYPE: ScalarType = object : ScalarType {
      override fun typeName(): String {
        return URL::class.java.name
      }

      override fun className(): String {
        return URL::class.java.name
      }
    }
    private val DATE_TIME_FORMAT = SimpleDateFormat("yyyyy-mm-dd")
  }
}
