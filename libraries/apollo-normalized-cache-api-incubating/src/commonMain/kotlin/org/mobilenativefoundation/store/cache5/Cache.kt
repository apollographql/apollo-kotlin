package org.mobilenativefoundation.store.cache5

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
