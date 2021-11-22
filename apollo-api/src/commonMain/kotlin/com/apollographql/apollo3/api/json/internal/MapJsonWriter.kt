package com.apollographql.apollo3.api.json.internal

import com.apollographql.apollo3.annotations.ApolloInternal
import com.apollographql.apollo3.api.Upload
import com.apollographql.apollo3.api.json.JsonNumber
import com.apollographql.apollo3.api.json.JsonWriter

/**
 * A [JsonWriter] that writes data to a [Map<String, Any?>]
 *
 * Call [beginObject], [name], [value], etc... like for regular Json writing. Once you're done, get the result in [root]
 *
 * The returned [Map] will contain values of the following types:
 * - String
 * - Int
 * - Double
 * - Long
 * - JsonNumber
 * - null
 * - Map<String, Any?> where values are any of these values recursively
 * - List<Any?> where values are any of these values recursively
 *
 * To write to a [okio.BufferedSink], see also [BufferedSinkJsonWriter]
 */
@ApolloInternal
class MapJsonWriter : JsonWriter {
  sealed class State {
    class List(val list: MutableList<Any?>) : State()
    class Map(val map: MutableMap<String, Any?>, var name: String?) : State()
  }

  private var root: Any? = null
  private var rootSet = false

  private val stack = mutableListOf<State>()

  /**
   * Return the resulting representation of the Json as a Kotlin type. Most of the times, it will be a [Map]<String, Any?> but
   * [MapJsonWriter] also support writing lists and scalars at the root of the json
   */
  fun root(): Any? {
    check(rootSet)
    return root
  }

  override fun beginArray(): JsonWriter = apply {
    val list = mutableListOf<Any?>()
    valueInternal(list)
    stack.add(State.List(list))
  }

  override fun endArray(): JsonWriter = apply {
    val state = stack.removeAt(stack.size - 1)

    check(state is State.List)
  }

  override fun beginObject(): JsonWriter = apply {
    val map = mutableMapOf<String, Any?>()

    valueInternal(map)

    stack.add(State.Map(map, null))
  }

  override fun endObject(): JsonWriter = apply {
    val state = stack.removeAt(stack.size - 1)

    check(state is State.Map)
  }

  override fun name(name: String): JsonWriter = apply {
    val state = stack.last()

    check(state is State.Map)
    check(state.name == null)

    state.name = name
  }

  private fun <T> valueInternal(value: T?) = apply {
    when (val state = stack.lastOrNull()) {
      is State.Map -> {
        check(state.name != null)

        // We support writing the same fields several time for fragments as classes
        val existingValue = state.map[state.name!!]
        if (existingValue != null && existingValue is Map<*, *>) {
          check(value is Map<*, *>)
          // merge any incoming object
          state.map[state.name!!] = existingValue + value
        } else {
          // just overwrite what was previously there
          // by construction it should be either null or the same value
          state.map[state.name!!] = value
        }
        state.name = null
      }
      is State.List -> {
        state.list.add(value)
      }
      else -> {
        root = value
        rootSet = true
      }
    }
  }

  override val path: String
    get() {
      return stack.map {
        when(it) {
          is State.List -> it.list.size.toString()
          is State.Map -> it.name ?: "?" // if we don't know the name display '?' for now
        }
      }.joinToString(".")
    }
  override fun value(value: String) = valueInternal(value)

  override fun value(value: Boolean) = valueInternal(value)

  override fun value(value: Double) = valueInternal(value)

  override fun value(value: Int) = valueInternal(value)

  override fun value(value: Long) = valueInternal(value)

  override fun value(value: JsonNumber) = valueInternal(value)

  override fun value(value: Upload) = valueInternal(null)

  override fun nullValue() = valueInternal(null)

  override fun close() {
  }

  override fun flush() {
  }
}