package com.apollographql.apollo.api

import com.apollographql.apollo.api.internal.json.JsonWriter
import com.apollographql.apollo.api.internal.json.Utils
import com.apollographql.apollo.api.internal.json.use
import okio.Buffer

/**
 * Builtin CustomScalarAdapter provided for convenience. Encoding is most of the times straightforward. Decoding
 * can involve coercion. If you need stricter decoding or different logic, define your own adapter
 */
object BuiltinScalarTypeAdapters {
  val STRING_ADAPTER = adapterWithDefaultEncode { jsonElement ->
    when (jsonElement) {
      is JsonElement.JsonString -> jsonElement.value
      is JsonElement.JsonNull -> null
      is JsonElement.JsonBoolean -> jsonElement.value.toString()
      is JsonElement.JsonNumber -> jsonElement.value.toString()
      is JsonElement.JsonObject,
      is JsonElement.JsonList -> {
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
      is JsonElement.JsonBoolean -> jsonElement.value
      is JsonElement.JsonString -> jsonElement.value.toBoolean()
      else -> throw IllegalArgumentException("Can't decode: $jsonElement into Boolean")
    }
  }

  val INT_ADAPTER = adapterWithDefaultEncode { jsonElement ->
    when (jsonElement) {
      is JsonElement.JsonNumber -> jsonElement.value.toInt()
      is JsonElement.JsonString -> jsonElement.value.toInt()
      else -> throw IllegalArgumentException("Can't decode: $jsonElement into Integer")
    }
  }

  val LONG_ADAPTER = adapterWithDefaultEncode { jsonElement ->
    when (jsonElement) {
      is JsonElement.JsonNumber -> jsonElement.value.toLong()
      is JsonElement.JsonString -> jsonElement.value.toLong()
      else -> throw IllegalArgumentException("Can't decode: $jsonElement into Long")
    }
  }

  val FLOAT_ADAPTER = adapterWithDefaultEncode { jsonElement ->
    when (jsonElement) {
      is JsonElement.JsonNumber -> jsonElement.value.toFloat()
      is JsonElement.JsonString -> jsonElement.value.toFloat()
      else -> throw IllegalArgumentException("Can't decode: $jsonElement into Float")
    }
  }

  val DOUBLE_ADAPTER = adapterWithDefaultEncode { jsonElement ->
    when (jsonElement) {
      is JsonElement.JsonNumber -> jsonElement.value.toDouble()
      is JsonElement.JsonString -> jsonElement.value.toDouble()
      else -> throw IllegalArgumentException("Can't decode: $jsonElement into Double")
    }
  }

  val MAP_ADAPTER = adapterWithDefaultEncode { jsonElement ->
    if (jsonElement is JsonElement.JsonObject) {
      jsonElement.toRawValue() as Map<String, Any?>
    } else {
      throw IllegalArgumentException("Can't decode: $jsonElement into Map")
    }
  }

  val LIST_ADAPTER = adapterWithDefaultEncode { jsonElement ->
    if (jsonElement is JsonElement.JsonList) {
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
      return JsonElement.JsonNull
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