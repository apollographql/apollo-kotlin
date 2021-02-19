package com.apollographql.apollo3.api.internal

import com.apollographql.apollo3.api.internal.json.JsonWriter

class MapJsonWriter: JsonWriter() {
  sealed class State {
    class List(val list: MutableList<Any?>): State()
    class Map(val map: MutableMap<String, Any?>, var name: String?): State()
  }

  private var root: Any? = null
  private var rootSet = false

  val stack = mutableListOf<State>()

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
    val state = stack.lastOrNull()

    if (state is State.Map) {
      check(state.name != null)
      state.map[state.name!!] = value
      state.name = null
    } else if( state is State.List) {
      state.list.add(value)
    } else {
      root = value
      rootSet = true
    }
  }
  override fun value(value: String?) = valueInternal(value)

  override fun value(value: Boolean?) = valueInternal(value)

  override fun value(value: Double) = valueInternal(value)

  override fun value(value: Long) = valueInternal(value)

  override fun value(value: Number?) = valueInternal(value)

  override fun jsonValue(value: String?) = throw UnsupportedOperationException()

  override fun nullValue() = valueInternal(null)

  override fun close() {
  }

  override fun flush() {
  }
}