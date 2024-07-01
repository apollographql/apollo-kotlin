package com.apollographql.apollo.api.json

import com.apollographql.apollo.api.Upload

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
class MapJsonWriter : JsonWriter {
  internal sealed class State {
    class List(val list: MutableList<Any?>) : State() {
      override fun toString(): String {
        return "List (${list.size})"
      }
    }

    class Map(val map: MutableMap<String, Any?>, var name: String?) : State() {
      override fun toString(): String {
        return "Map ($name)"
      }
    }
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
    stack.add(State.List(list))
  }

  override fun endArray(): JsonWriter = apply {
    val state = stack.removeAt(stack.size - 1)
    check(state is State.List)

    valueInternal(state.list)
  }

  override fun beginObject(): JsonWriter = apply {
    val map = mutableMapOf<String, Any?>()

    stack.add(State.Map(map, null))
  }

  private fun Any?.mergeWith(other: Any?): Any? {
    if (this == null) {
      return other
    }
    if (other == null) {
      return this
    }
    return when (this) {
      is List<*> -> {
        check(other is List<*>) {
          "Cannot merge $this with $other"
        }
        check(size == other.size) {
          "Cannot merge $this with $other"
        }
        indices.map { i ->
          get(i).mergeWith(other.get(i))
        }
      }
      is Map<*, *> -> {
        check(other is Map<*, *>) {
          "Cannot merge $this with $other"
        }
        @Suppress("UNCHECKED_CAST")
        this as Map<String, Any?>
        @Suppress("UNCHECKED_CAST")
        other as Map<String, Any?>

        (keys + other.keys).map {
          it to get(it).mergeWith(other.get(it))
        }.toMap()
      }
      else -> {
        check (this == other) {
          error("Cannot merge $this with $other")
        }
        this
      }
    }
  }

  override fun endObject(): JsonWriter = apply {
    val state = stack.removeAt(stack.size - 1)
    check(state is State.Map)

    valueInternal(state.map)
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
        val name = state.name
        check(name != null)
        if (state.map.containsKey(name)) {
          // There is already a value. This happens when using fragments and operationBased codegen
          // when we have to rewind the parser
          state.map[name] = state.map[name].mergeWith(value)
        } else {
          state.map[name] = value
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
        when (it) {
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

  fun value(value: Any?) = valueInternal(value)

  override fun nullValue() = valueInternal(null)

  override fun close() {
  }

  override fun flush() {
  }
}
