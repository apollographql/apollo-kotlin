@file:JvmName("-JsonReaders")

package com.apollographql.apollo.api.json

import com.apollographql.apollo.annotations.ApolloInternal
import com.apollographql.apollo.api.json.internal.toIntExactOrNull
import com.apollographql.apollo.api.json.internal.toLongExactOrNull
import okio.BufferedSource
import kotlin.jvm.JvmName

fun BufferedSource.jsonReader(): JsonReader {
  return BufferedSourceJsonReader(this)
}

fun Map<String, Any?>.jsonReader(): JsonReader {
  return MapJsonReader(root = this)
}

/**
 * Returns the Kotlin representation of the given [JsonReader]
 *
 * JSON numbers are mapped to built-in types when possible in that order:
 * - Int
 * - Long
 * - Double
 * - JsonNumber
 */
@ApolloInternal
fun JsonReader.readAny(): ApolloJsonElement {
  return when (val token = peek()) {
    JsonReader.Token.NULL -> nextNull()
    JsonReader.Token.BOOLEAN -> nextBoolean()
    JsonReader.Token.LONG, JsonReader.Token.NUMBER -> guessNumber()
    JsonReader.Token.STRING -> nextString()
    JsonReader.Token.BEGIN_OBJECT -> {
      beginObject()
      val result = mutableMapOf<String, Any?>()
      while (hasNext()) {
        result.put(nextName(), readAny())
      }
      endObject()
      result
    }
    JsonReader.Token.BEGIN_ARRAY -> {
      beginArray()
      val result = mutableListOf<Any?>()
      while (hasNext()) {
        result.add(readAny())
      }
      endArray()
      result
    }
    else -> error("unknown token $token")
  }
}

private fun JsonReader.guessNumber(): Any {
  /**
   * The generic implementation below relies on exceptions for control flow, which is very costly
   * when parsing large responses. The two built-in readers know their number representation, so
   * they can narrow the value without ever throwing. Other [JsonReader] implementations fall back
   * to the generic path.
   */
  return when (this) {
    is MapJsonReader -> nextGuessedNumber()
    is BufferedSourceJsonReader -> guessNumberNoThrow()
    else -> guessNumberOrThrow()
  }
}

/**
 * Same as [guessNumberOrThrow] but without using exceptions for control flow.
 *
 * [BufferedSourceJsonReader] only uses [JsonReader.Token.LONG] for values that fit a `Long`, so
 * anything else has to go through a `Double`.
 */
private fun BufferedSourceJsonReader.guessNumberNoThrow(): Any {
  if (peek() == JsonReader.Token.LONG) {
    val asLong = nextLong()
    return asLong.toIntExactOrNull() ?: asLong
  }

  /**
   * XXX: this can lose precision on large numbers (String.toDouble() may approximate)
   * This hasn't been an issue so far, and it's used quite extensively, so I'm keeping it
   * as is for the time being.
   * If you're reading this, it probably means it became an issue. In that case, nextDouble()
   * here should be skipped and the calling code be adapted.
   */
  val asDouble = try {
    nextDouble()
  } catch (_: Exception) {
    // NaN, Infinity, or a value that isn't a valid Double: keep the original representation
    return nextNumber()
  }

  return asDouble.toIntExactOrNull() ?: asDouble.toLongExactOrNull() ?: asDouble
}

private fun JsonReader.guessNumberOrThrow(): Any {
  try {
    return nextInt()
  } catch (_: Exception) {
  }
  try {
    return nextLong()
  } catch (_: Exception) {
  }
  try {
    return nextDouble()
  } catch (_: Exception) {
  }
  return nextNumber()
}

