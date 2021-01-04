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

import com.apollographql.apollo.api.internal.Utils.__checkNotNull

/**
 * Implementation of an [Optional] not containing a reference.
 */
internal class Absent<T> private constructor() : Optional<T>() {
  override val isPresent = false

  override fun get(): T {
    throw IllegalStateException("Optional.get() cannot be called on an absent value")
  }

  override fun or(defaultValue: T): T {
    return __checkNotNull(defaultValue, "use Optional.orNull() instead of Optional.or(null)")
  }

  override fun or(secondChoice: Optional<out T>): Optional<T> {
    return __checkNotNull(secondChoice) as Optional<T>
  }

  override fun orNull(): T? {
    return null
  }

  override fun <V> transform(function: Function<in T, V>): Optional<V> {
    __checkNotNull(function)
    return absent()
  }

  override fun <V> map(function: Function<in T, V>): Optional<V> {
    __checkNotNull(function)
    return absent()
  }

  override fun <V> flatMap(function: Function<in T, Optional<V>>): Optional<V> {
    __checkNotNull(function)
    return absent()
  }

  override fun apply(action: Action<T>): Optional<T> {
    __checkNotNull(action)
    return absent()
  }

  override fun asSet(): Set<T> {
    return emptySet()
  }

  override fun equals(`object`: Any?): Boolean {
    return `object` === this
  }

  override fun hashCode(): Int {
    return 0x79a31aac
  }

  override fun toString(): String {
    return "Optional.absent()"
  }

  private fun readResolve(): Any {
    return INSTANCE
  }

  companion object {
    val INSTANCE = Absent<Any>()
    @JvmStatic
    fun <T> withType(): Optional<T> {
      return INSTANCE as Optional<T>
    }

    private const val serialVersionUID: Long = 0
  }
}