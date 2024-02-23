/*
 * Taken from the Mobile Native Foundation Store project.
 * https://github.com/MobileNativeFoundation/Store/commit/e25e3c130187d9294ad5b998136b0498bd91d88f
 *
 * Copyright (c) 2017 The New York Times Company
 *
 * Copyright (C) 2009 The Guava Authors
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
package com.apollographql.apollo3.cache.normalized.api.internal.store

interface Cache<Key : Any, Value : Any> {
    /**
     * @return [Value] associated with [key] or `null` if there is no cached value for [key].
     */
    fun getIfPresent(key: Key): Value?

    /**
     * @return [Value] associated with [key], obtaining the value from [valueProducer] if necessary.
     * No observable state associated with this cache is modified until loading completes.
     * @param [valueProducer] Must not return `null`. It may either return a non-null value or throw an exception.
     * @throws ExecutionExeption If a checked exception was thrown while loading the value.
     * @throws UncheckedExecutionException If an unchecked exception was thrown while loading the value.
     * @throws ExecutionError If an error was thrown while loading the value.
     */
    fun getOrPut(key: Key, valueProducer: () -> Value): Value

    /**
     * @return Map of the [Value] associated with each [Key] in [keys]. Returned map only contains entries already present in the cache.
     */
    fun getAllPresent(keys: List<*>): Map<Key, Value>

    /**
     * @return Map of the [Value] associated with each [Key] in the cache.
     */
    fun getAllPresent(): Map<Key, Value>

    /**
     * Associates [value] with [key].
     * If the cache previously contained a value associated with [key], the old value is replaced by [value].
     * Prefer [getOrPut] when using the conventional "If cached, then return. Otherwise create, cache, and then return" pattern.
     */
    fun put(key: Key, value: Value)

    /**
     * Copies all of the mappings from the specified map to the cache. The effect of this call is
     * equivalent to that of calling [put] on this map once for each mapping from [Key] to [Value] in the specified map.
     * The behavior of this operation is undefined if the specified map is modified while the operation is in progress.
     */
    fun putAll(map: Map<Key, Value>)

    /**
     * Discards any cached value associated with [key].
     */
    fun invalidate(key: Key)

    /**
     * Discards any cached value associated for [keys].
     */
    fun invalidateAll(keys: List<Key>)

    /**
     * Discards all entries in the cache.
     */
    fun invalidateAll()

    /**
     * @return Approximate number of entries in the cache.
     */
    fun size(): Long
}
