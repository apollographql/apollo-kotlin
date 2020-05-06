package com.apollographql.apollo.api

import com.apollographql.apollo.api.ExecutionContext.Element
import com.apollographql.apollo.api.ExecutionContext.Key
import kotlin.jvm.JvmField

/**
 * A context of GraphQL operation execution, represented as a set of [Key] keys and corresponding [Element] values.
 */
class ExecutionContext private constructor(private val context: Map<Key<*>, Element>) {

  /**
   * Returns the element with the given [key] from this context or `null`.
   */
  @Suppress("UNCHECKED_CAST")
  operator fun <E : Element> get(key: Key<E>): E? {
    return context[key] as? E
  }

  /**
   * Sets the element with the given [key] and returns a new context.
   */
  operator fun <E : Element> set(key: Key<E>, element: E): ExecutionContext {
    return ExecutionContext(context + (key to element))
  }

  /**
   * Key for the elements of [ExecutionContext]. [E] is a type of element with this key.
   */
  interface Key<E : Element>

  /**
   * An element of the [ExecutionContext].
   */
  interface Element

  companion object {
    @JvmField
    val Empty: ExecutionContext = ExecutionContext(emptyMap())
  }
}
