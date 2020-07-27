package com.apollographql.apollo.api.internal.json

import com.apollographql.apollo.api.BigDecimal
import com.apollographql.apollo.api.internal.Throws
import okio.IOException

open class ResponseJsonStreamReader(
    private val jsonReader: JsonReader
) {

  @Throws(IOException::class)
  operator fun hasNext(): Boolean = jsonReader.hasNext()

  @Throws(IOException::class)
  fun nextName(): String = jsonReader.nextName()

  @Throws(IOException::class)
  fun skipNext() {
    jsonReader.skipValue()
  }

  @Throws(IOException::class)
  fun nextLong(optional: Boolean): Long? {
    checkNextValue(optional)
    return if (jsonReader.peek() === JsonReader.Token.NULL) {
      jsonReader.nextNull()
    } else {
      jsonReader.nextLong()
    }
  }

  @Throws(IOException::class)
  fun nextString(optional: Boolean): String? {
    checkNextValue(optional)
    return if (jsonReader.peek() === JsonReader.Token.NULL) {
      jsonReader.nextNull()
    } else {
      jsonReader.nextString()
    }
  }

  @Throws(IOException::class)
  fun nextBoolean(optional: Boolean): Boolean? {
    checkNextValue(optional)
    return if (jsonReader.peek() === JsonReader.Token.NULL) {
      jsonReader.nextNull()
    } else {
      jsonReader.nextBoolean()
    }
  }

  @Throws(IOException::class)
  fun <T> nextObject(optional: Boolean, objectReader: ObjectReader<T>): T? {
    checkNextValue(optional)
    return if (jsonReader.peek() === JsonReader.Token.NULL) {
      jsonReader.nextNull()
    } else {
      jsonReader.beginObject()
      val result = objectReader.read(this)
      jsonReader.endObject()
      result
    }
  }

  @Throws(IOException::class)
  fun <T> nextList(optional: Boolean, listReader: ListReader<T>): List<T?>? {
    checkNextValue(optional)
    return if (jsonReader.peek() === JsonReader.Token.NULL) {
      jsonReader.nextNull()
    } else {
      jsonReader.beginArray()
      val result = ArrayList<T?>()
      while (jsonReader.hasNext()) {
        result.add(listReader.read(this))
      }
      jsonReader.endArray()
      result
    }
  }

  @Throws(IOException::class)
  open fun nextScalar(optional: Boolean): Any? {
    checkNextValue(optional)
    return when {
      isNextNull -> skipNext().let { null }
      isNextBoolean -> nextBoolean(false)
      isNextLong -> BigDecimal(nextLong(false)!!)
      isNextNumber -> BigDecimal(nextString(false)!!)
      else -> nextString(false)
    }
  }

  @Throws(IOException::class)
  fun readObject(): Map<String, Any?>? {
    return nextObject(false, object : ObjectReader<Map<String, Any?>?> {
      @Throws(IOException::class)
      override fun read(reader: ResponseJsonStreamReader) = reader.toMap()
    })
  }

  @Throws(IOException::class)
  fun readList(): List<Any?>? {
    return nextList(false, object : ListReader<Any?> {
      @Throws(IOException::class)
      override fun read(reader: ResponseJsonStreamReader) = when {
        isNextList -> readList()
        isNextObject -> readObject()
        else -> reader.nextScalar(true)
      }
    })
  }

  @Throws(IOException::class)
  fun toMap(): Map<String, Any?>? {
    if (isNextObject) {
      return readObject()
    }
    val result = LinkedHashMap<String, Any?>()
    while (hasNext()) {
      val name = nextName()
      when {
        isNextNull -> skipNext().also { result[name] = null }
        isNextObject -> result[name] = readObject()
        isNextList -> result[name] = readList()
        else -> result[name] = nextScalar(true)
      }
    }
    return result
  }

  @get:Throws(IOException::class)
  val isNextObject: Boolean
    get() = jsonReader.peek() === JsonReader.Token.BEGIN_OBJECT

  @get:Throws(IOException::class)
  val isNextList: Boolean
    get() = jsonReader.peek() === JsonReader.Token.BEGIN_ARRAY

  @get:Throws(IOException::class)
  private val isNextNull: Boolean
    get() = jsonReader.peek() === JsonReader.Token.NULL

  @get:Throws(IOException::class)
  private val isNextBoolean: Boolean
    get() = jsonReader.peek() === JsonReader.Token.BOOLEAN

  @get:Throws(IOException::class)
  private val isNextNumber: Boolean
    get() = jsonReader.peek() === JsonReader.Token.NUMBER

  @get:Throws(IOException::class)
  private val isNextLong: Boolean
    get() = jsonReader.peek() === JsonReader.Token.LONG

  @Throws(IOException::class)
  private fun checkNextValue(optional: Boolean) {
    if (!optional && jsonReader.peek() === JsonReader.Token.NULL) {
      throw NullPointerException("corrupted response reader, expected non null value")
    }
  }

  interface ObjectReader<T> {
    @Throws(IOException::class)
    fun read(reader: ResponseJsonStreamReader): T
  }

  interface ListReader<T> {
    @Throws(IOException::class)
    fun read(reader: ResponseJsonStreamReader): T?
  }
}
