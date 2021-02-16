/*
 * Copyright (C) 2011 The Guava Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.apollographql.apollo.api.internal


/**
 * Implementation of an [Optional] containing a reference.
 */
internal class Present<T>(private val reference: T) : Optional<T>() {
  override val isPresent: Boolean
    get() = true

  override fun get(): T {
    return reference
  }

  override fun or(defaultValue: T): T {
    return reference
  }

  override fun or(secondChoice: Optional<out T>): Optional<T> {
    return this
  }

  override fun <V> transform(function: Function<in T, V>): Optional<V> {
    return Present(function.apply(reference))
  }

  override fun <V> map(function: Function<in T, V>): Optional<V> {
    return Present(function.apply(reference))
  }

  override fun <V> flatMap(function: Function<in T, Optional<V>>): Optional<V> {
    return function.apply(reference)
  }

  override fun apply(action: Action<T>): Optional<T> {
    return map(object : Function<T, T> {
      override fun apply(t: T): T {
        action.apply(t)
        return t
      }
    })
  }

  override fun orNull(): T? {
    return reference
  }

  override fun asSet(): Set<T> {
    return setOf(reference)
  }

  override fun equals(`object`: Any?): Boolean {
    if (`object` is Present<*>) {
      return reference == `object`.reference
    }
    return false
  }

  override fun hashCode(): Int {
    return 0x598df91c + reference.hashCode()
  }

  override fun toString(): String {
    return "Optional.of($reference)"
  }

  companion object {
    private const val serialVersionUID: Long = 0
  }
}