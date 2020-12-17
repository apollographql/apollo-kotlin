package com.apollographql.apollo.subscription

import com.apollographql.apollo.api.internal.json.JsonWriter
import com.apollographql.apollo.api.internal.json.writeArray
import com.apollographql.apollo.api.internal.json.writeObject

internal fun JsonWriter.jsonValue(value: Any?) {
  when (value) {
    is Map<*, *> -> writeObject {
      for ((k, v) in value) {
        name(k as String).jsonValue(v)
      }
    }
    is List<*> -> writeArray {
      for (v in value) {
        jsonValue(v)
      }
    }
    is Boolean -> value(value)
    is Number -> value(value)
    is String -> value(value)
    null -> nullValue()
    else -> throw IllegalArgumentException("$value is not a valid JSON type")
  }
}