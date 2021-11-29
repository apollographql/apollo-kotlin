@file:JvmName("-JsonReaders")
package com.apollographql.apollo3.api.json

import com.apollographql.apollo3.annotations.ApolloInternal
import okio.BufferedSource
import kotlin.jvm.JvmName

fun BufferedSource.jsonReader(): JsonReader {
  return BufferedSourceJsonReader(this)
}

fun Map<String, Any?>.jsonReader(): JsonReader {
  return MapJsonReader(this)
}

/**
 * Reads the reader and maps numbers to the closest representation possible in that order:
 * - Int
 * - Long
 * - Double
 * - JsonNumber
 */
@ApolloInternal
fun JsonReader.readAny(): Any? {
  return when(val token = peek()) {
    JsonReader.Token.NULL -> nextNull()
    JsonReader.Token.BOOLEAN -> nextBoolean()
    JsonReader.Token.LONG, JsonReader.Token.NUMBER -> guessNumber()
    JsonReader.Token.STRING -> nextString()
    JsonReader.Token.BEGIN_OBJECT -> {
      beginObject()
      val result = mutableMapOf<String, Any?>()
      while(hasNext()) {
        result.put(nextName(), readAny())
      }
      endObject()
      result
    }
    JsonReader.Token.BEGIN_ARRAY -> {
      beginArray()
      val result = mutableListOf<Any?>()
      while(hasNext()) {
        result.add(readAny())
      }
      endArray()
      result
    }
    else -> error("unknown token $token")
  }
}

private fun JsonReader.guessNumber(): Any {
  try {
    return nextInt()
  } catch (e: Exception) {
  }
  try {
    return nextLong()
  } catch (e: Exception) {
  }
  try {
    return nextDouble()
  } catch (e: Exception) {
  }
  return nextNumber()
}