package com.apollographql.apollo3.api

import com.apollographql.apollo3.api.internal.json.JsonWriter
import com.apollographql.apollo3.api.internal.json.Utils
import com.apollographql.apollo3.api.internal.json.use
import okio.Buffer

/**
 * Builtin CustomScalarAdapter provided for convenience. Encoding is most of the times straightforward. Decoding
 * can involve coercion. If you need stricter decoding or different logic, define your own adapter
 */
object BuiltinCustomScalarAdapters {
  val STRING_ADAPTER = adapterWithDefaultEncode { jsonElement ->
    when (jsonElement) {
      is JsonString -> jsonElement.value
      is JsonNull -> null
      is JsonBoolean -> jsonElement.value.toString()
      is JsonNumber -> jsonElement.value.toString()
      is JsonObject,
      is JsonList -> {
        val buffer = Buffer()
        JsonWriter.of(buffer).use { writer ->
          Utils.writeToJson(jsonElement.toRawValue(), writer)
        }
        buffer.readUtf8()
      }
    }
  }

  val BOOLEAN_ADAPTER = adapterWithDefaultEncode { jsonElement ->
    when (jsonElement) {
      is JsonBoolean -> jsonElement.value
      is JsonString -> jsonElement.value.toBoolean()
      else -> throw IllegalArgumentException("Can't decode: $jsonElement into Boolean")
    }
  }

  val INT_ADAPTER = adapterWithDefaultEncode { jsonElement ->
    when (jsonElement) {
      is JsonNumber -> jsonElement.value.toInt()
      is JsonString -> jsonElement.value.toInt()
      else -> throw IllegalArgumentException("Can't decode: $jsonElement into Integer")
    }
  }

  val LONG_ADAPTER = adapterWithDefaultEncode { jsonElement ->
    when (jsonElement) {
      is JsonNumber -> jsonElement.value.toLong()
      is JsonString -> jsonElement.value.toLong()
      else -> throw IllegalArgumentException("Can't decode: $jsonElement into Long")
    }
  }

  val FLOAT_ADAPTER = adapterWithDefaultEncode { jsonElement ->
    when (jsonElement) {
      is JsonNumber -> jsonElement.value.toFloat()
      is JsonString -> jsonElement.value.toFloat()
      else -> throw IllegalArgumentException("Can't decode: $jsonElement into Float")
    }
  }

  val DOUBLE_ADAPTER = adapterWithDefaultEncode { jsonElement ->
    when (jsonElement) {
      is JsonNumber -> jsonElement.value.toDouble()
      is JsonString -> jsonElement.value.toDouble()
      else -> throw IllegalArgumentException("Can't decode: $jsonElement into Double")
    }
  }

  val MAP_ADAPTER = adapterWithDefaultEncode { jsonElement ->
    if (jsonElement is JsonObject) {
      jsonElement.toRawValue() as Map<String, Any?>
    } else {
      throw IllegalArgumentException("Can't decode: $jsonElement into Map")
    }
  }

  val LIST_ADAPTER = adapterWithDefaultEncode { jsonElement ->
    if (jsonElement is JsonList) {
      jsonElement.toRawValue() as List<Any?>
    } else {
      throw IllegalArgumentException("Can't decode: $jsonElement into List")
    }
  }

  val FALLBACK_ADAPTER = adapterWithDefaultEncode { jsonElement ->
    jsonElement.toRawValue()
  }

  val FILE_UPLOAD_ADAPTER = object : CustomScalarAdapter<FileUpload> {
    override fun decode(jsonElement: JsonElement): FileUpload {
      throw IllegalStateException("ApolloGraphQL: cannot decode FileUpload")
    }

    override fun encode(value: FileUpload): JsonElement {
      return JsonNull
    }
  }
}

private fun <T> adapterWithDefaultEncode(
    decode: (jsonElement: JsonElement) -> T
): CustomScalarAdapter<T> {
  return object : CustomScalarAdapter<T> {
    override fun decode(jsonElement: JsonElement): T {
      return decode(jsonElement)
    }

    override fun encode(value: T): JsonElement {
      return JsonElement.fromRawValue(value)
    }
  }
}