package com.apollographql.apollo.cache.normalized.internal

import com.apollographql.apollo.api.internal.json.BufferedSourceJsonReader
import com.apollographql.apollo.api.internal.json.JsonReader
import com.apollographql.apollo.cache.CacheHeaders
import com.apollographql.apollo.cache.normalized.CacheReference
import okio.Buffer

class CacheJsonReader(
    private val rootKey: String,
    private val readableCache: ReadableStore,
    private val cacheHeaders: CacheHeaders,
): JsonReader {
  private var currentReader: JsonReader = BufferedSourceJsonReader(Buffer().writeUtf8("\"ApolloCacheReference{$rootKey}\""))
  private var currentDepth = 0

  private val readerStack = ArrayList<JsonReader>()
  private val depthStack = ArrayList<Int>()

  private fun push(reader: JsonReader) {
    readerStack.add(currentReader)
    depthStack.add(currentDepth)

    currentReader = reader
    currentReader.beginObject()
    currentDepth = 0
  }

  private fun pop() {
    currentReader = readerStack.removeAt(readerStack.size - 1)
    currentDepth = depthStack.removeAt(depthStack.size - 1)
  }

  override fun beginArray() = apply {
    currentReader.beginArray()
  }

  override fun endArray() = apply {
    currentReader.endArray()
  }

  override fun beginObject() = apply {
    if (currentReader.peek() == JsonReader.Token.STRING) {
      val ref = currentReader.nextString()!!
      val cacheKey = CacheReference.deserialize(ref)

      val nextReader = readableCache.stream(cacheKey.key, cacheHeaders)

      check(nextReader != null) {
        "cache MISS on ${cacheKey.key}"
      }
      push(nextReader)
    } else if (currentReader.peek() == JsonReader.Token.BEGIN_OBJECT){
      // nested scalar field
      currentReader.beginObject()
    } else {
      error("not an object or a string :(")
    }
  }

  override fun endObject() = apply {
    currentReader.endObject()
    if (currentReader.peek() == JsonReader.Token.END_DOCUMENT) {
      pop()
    }
  }

  override fun hasNext(): Boolean {
    return currentReader.hasNext()
  }

  override fun peek(): JsonReader.Token {
    // We should return BEING_OBJECT on CacheReferences but noone is checking for this
    // at the moment
    return currentReader.peek()
  }

  override fun nextName(): String {
    return currentReader.nextName()
  }

  override fun nextString(): String? {
    return currentReader.nextString()
  }

  override fun nextBoolean(): Boolean {
    return currentReader.nextBoolean()
  }

  override fun <T> nextNull(): T? {
    return currentReader.nextNull()
  }

  override fun nextDouble(): Double {
    return currentReader.nextDouble()
  }

  override fun nextLong(): Long {
    return currentReader.nextLong()
  }

  override fun nextInt(): Int {
    return currentReader.nextInt()
  }

  override fun skipValue() {
    return currentReader.skipValue()
  }

  override fun close() {
    // Nothing to do \o/
  }
}