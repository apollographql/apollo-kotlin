package com.apollographql.apollo.tooling

import com.apollographql.apollo.tooling.SchemaDownloader.cast
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.longOrNull

internal fun JsonElement.toAny(): Any? = when (this) {
  is JsonObject -> this.mapValues { it.value.toAny() }
  is JsonArray -> this.map { it.toAny() }
  is JsonPrimitive -> {
    when {
      isString -> this.content
      this is JsonNull -> null
      else -> booleanOrNull ?: intOrNull ?: longOrNull ?: doubleOrNull ?: error("cannot decode $this")
    }
  }
  else -> error("cannot convert $this to Any")
}

fun Any?.toJsonElement(): JsonElement = when (this) {
  is Map<*, *> -> JsonObject(this.cast<Map<String, *>>()!!.mapValues { it.value.toJsonElement() })
  is List<*> -> JsonArray(map { it.toJsonElement() })
  is Boolean -> JsonPrimitive(this)
  is Number -> JsonPrimitive(this)
  is String -> JsonPrimitive(this)
  null -> JsonNull
  else -> error("cannot convert $this to JsonElement")
}