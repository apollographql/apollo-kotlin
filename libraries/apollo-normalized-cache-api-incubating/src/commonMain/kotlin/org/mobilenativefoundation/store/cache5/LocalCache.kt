/*
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
 *
 * KMP conversion
 * Copyright (C) 2022 André Claßen
 */
package org.mobilenativefoundation.store.cache5

import kotlinx.atomicfu.AtomicArray
import kotlinx.atomicfu.AtomicRef
import kotlinx.atomicfu.atomic
import kotlinx.atomicfu.atomicArrayOfNulls
import kotlinx.atomicfu.locks.reentrantLock
import kotlinx.atomicfu.loop
import kotlin.math.min
import kotlin.time.Duration

internal class LocalCache<K : Any, V : Any>(builder: CacheBuilder<K, V>) {

    /**
     * Mask value for indexing into segments. The upper bits of a key's hash code are used to choose
     * the segment.
     */
    private val segmentMask: Int

    /**
     * Shift value for indexing within segments. Helps prevent entries that end up in the same segment
     * from also ending up in the same bucket.
     */
    private val segmentShift: Int

    /**
     * The segments, each of which is a specialized hash table.
     */

    private val segments: Array<Segment<K, V>?>

    /**
     * Strategy for referencing values.
     */
    private val valueStrength: Strength = Strength.Strong

    /**
     * The maximum weight of this map. UNSET_LONG if there is no maximum.
     */
    private val maxWeight: Long

    /**
     * Weigher to weigh cache entries.
     */
    private val weigher: Weigher<K, V>

    /**
     * How long after the last access to an entry the map will retain that entry.
     */
    private val expireAfterAccessNanos: Long

    /**
     * How long after the last write to an entry the map will retain that entry.
     */
    private val expireAfterWriteNanos: Long

    /**
     * Measures time in a testable way.
     */
    private val ticker: Ticker

    /**
     * Factory used to create new entries.
     */
    private val entryFactory: EntryFactory

    private val evictsBySize: Boolean get() = maxWeight >= 0

    private val customWeigher: Boolean get() = weigher !== OneWeigher

    private val expiresAfterWrite: Boolean get() = expireAfterWriteNanos > 0

    private val expiresAfterAccess: Boolean get() = expireAfterAccessNanos > 0

    private val usesAccessQueue: Boolean get() = expiresAfterAccess || evictsBySize

    private val usesWriteQueue: Boolean get() = expiresAfterWrite

    private val recordsWrite: Boolean get() = expiresAfterWrite

    private val recordsAccess: Boolean get() = expiresAfterAccess

    private val recordsTime: Boolean get() = recordsWrite || recordsAccess

    private val usesWriteEntries: Boolean get() = usesWriteQueue || recordsWrite

    private val usesAccessEntries: Boolean get() = usesAccessQueue || recordsAccess

    private sealed class Strength {
        /*
         * TODO(kevinb): If we strongly reference the value and aren't loading, we needn't wrap the
         * value. This could save ~8 bytes per entry.
         */
        object Strong : Strength() {
            override fun <K : Any, V : Any> referenceValue(
                segment: Segment<K, V>?,
                entry: ReferenceEntry<K, V>?,
                value: V,
                weight: Int
            ): ValueReference<K, V> {
                return if (weight == 1) StrongValueReference(value) else WeightedStrongValueReference(
                    value,
                    weight
                )
            }
        }

        /**
         * Creates a reference for the given value according to this value strength.
         */
        abstract fun <K : Any, V : Any> referenceValue(
            segment: Segment<K, V>?,
            entry: ReferenceEntry<K, V>?,
            value: V,
            weight: Int
        ): ValueReference<K, V>
    }

    /**
     * Creates new entries.
     */
    private sealed class EntryFactory {
        object Strong : EntryFactory() {
            override fun <K : Any, V : Any> newEntry(
                segment: Segment<K, V>?,
                key: K,
                hash: Int,
                next: ReferenceEntry<K, V>?
            ): ReferenceEntry<K, V> {
                return StrongEntry(key, hash, next)
            }
        }

        object StrongAccess : EntryFactory() {

            override fun <K : Any, V : Any> newEntry(
                segment: Segment<K, V>?,
                key: K,
                hash: Int,
                next: ReferenceEntry<K, V>?
            ): ReferenceEntry<K, V> {
                return StrongAccessEntry(key, hash, next)
            }

            override fun <K : Any, V : Any> copyEntry(
                segment: Segment<K, V>?,
                original: ReferenceEntry<K, V>,
                newNext: ReferenceEntry<K, V>?
            ): ReferenceEntry<K, V> {
                val newEntry = super.copyEntry(segment, original, newNext)
                copyAccessEntry(original, newEntry)
                return newEntry
            }
        }

        object StrongWrite : EntryFactory() {

            override fun <K : Any, V : Any> newEntry(
                segment: Segment<K, V>?,
                key: K,
                hash: Int,
                next: ReferenceEntry<K, V>?
            ): ReferenceEntry<K, V> {
                return StrongWriteEntry(key, hash, next)
            }

            override fun <K : Any, V : Any> copyEntry(
                segment: Segment<K, V>?,
                original: ReferenceEntry<K, V>,
                newNext: ReferenceEntry<K, V>?
            ): ReferenceEntry<K, V> {
                val newEntry = super.copyEntry(segment, original, newNext)
                copyWriteEntry(original, newEntry)
                return newEntry
            }
        }

        object StrongAccessWrite : EntryFactory() {

            override fun <K : Any, V : Any> newEntry(
                segment: Segment<K, V>?,
                key: K,
                hash: Int,
                next: ReferenceEntry<K, V>?
            ): ReferenceEntry<K, V> {
                return StrongAccessWriteEntry(key, hash, next)
            }

            override fun <K : Any, V : Any> copyEntry(
                segment: Segment<K, V>?,
                original: ReferenceEntry<K, V>,
                newNext: ReferenceEntry<K, V>?
            ): ReferenceEntry<K, V> {
                val newEntry = super.copyEntry(segment, original, newNext)
                copyAccessEntry(original, newEntry)
                copyWriteEntry(original, newEntry)
                return newEntry
            }
        }

        /**
         * Creates a new entry.
         *
         * @param segment to create the entry for
         * @param key     of the entry
         * @param hash    of the key
         * @param next    entry in the same bucket
         */
        abstract fun <K : Any, V : Any> newEntry(
            segment: Segment<K, V>?,
            key: K,
            hash: Int,
            next: ReferenceEntry<K, V>?
        ): ReferenceEntry<K, V>

        /**
         * Copies an entry, assigning it a new `next` entry.
         *
         * @param original the entry to copy
         * @param newNext  entry in the same bucket
         */
        // Guarded By Segment.this
        open fun <K : Any, V : Any> copyEntry(
            segment: Segment<K, V>?,
            original: ReferenceEntry<K, V>,
            newNext: ReferenceEntry<K, V>?
        ): ReferenceEntry<K, V> {
            return newEntry(segment, original.key, original.hash, newNext)
        }

        // Guarded By Segment.this
        fun <K : Any, V : Any> copyAccessEntry(
            original: ReferenceEntry<K, V>,
            newEntry: ReferenceEntry<K, V>
        ) {
            // TODO(fry): when we link values instead of entries this method can go
            // away, as can connectAccessOrder, nullifyAccessOrder.
            newEntry.accessTime = original.accessTime
            connectAccessOrder(original.previousInAccessQueue, newEntry)
            connectAccessOrder(newEntry, original.nextInAccessQueue)
            nullifyAccessOrder(original)
        }

        // Guarded By Segment.this
        fun <K : Any, V : Any> copyWriteEntry(
            original: ReferenceEntry<K, V>,
            newEntry: ReferenceEntry<K, V>
        ) {
            // TODO(fry): when we link values instead of entries this method can go
            // away, as can connectWriteOrder, nullifyWriteOrder.
            newEntry.writeTime = original.writeTime
            connectWriteOrder(original.previousInWriteQueue, newEntry)
            connectWriteOrder(newEntry, original.nextInWriteQueue)
            nullifyWriteOrder(original)
        }

        companion object {
            /**
             * Masks used to compute indices in the following table.
             */
            private const val ACCESS_MASK = 1
            private const val WRITE_MASK = 2

            /**
             * Look-up table for factories.
             */
            private val factories = arrayOf(Strong, StrongAccess, StrongWrite, StrongAccessWrite)
            fun getFactory(usesAccessQueue: Boolean, usesWriteQueue: Boolean): EntryFactory {
                val flags = ((if (usesAccessQueue) ACCESS_MASK else 0) or if (usesWriteQueue) WRITE_MASK else 0)
                return factories[flags]
            }
        }
    }

    /**
     * A reference to a value.
     */
    private interface ValueReference<K : Any, V : Any> {
        /**
         * Returns the value. Does not block or throw exceptions.
         */
        fun get(): V?

        /**
         * Returns the weight of this entry. This is assumed to be static between calls to setValue.
         */
        val weight: Int

        /**
         * Returns the entry associated with this value reference, or `null` if this value
         * reference is independent of any entry.
         */
        val entry: ReferenceEntry<K, V>?

        /**
         * Creates a copy of this reference for the given entry.
         *
         *
         *
         * `value` may be null only for a loading reference.
         */

        fun copyFor(value: V?, entry: ReferenceEntry<K, V>?): ValueReference<K, V>

        /**
         * Notifify pending loads that a new value was set. This is only relevant to loading
         * value references.
         */
        fun notifyNewValue(newValue: V)

        /**
         * Returns true if this reference contains an active value, meaning one that is still considered
         * present in the cache. Active values consist of live values, which are returned by cache
         * lookups, and dead values, which have been evicted but awaiting removal. Non-active values
         * consist strictly of loading values, though during refresh a value may be both active and
         * loading.
         */
        val isActive: Boolean
    }

    /**
     * An entry in a reference map.
     *
     *
     * Entries in the map can be in the following states:
     *
     *
     * Valid:
     * - Live: valid key/value are set
     * - Loading: loading is pending
     *
     *
     * Invalid:
     * - Expired: time expired (key/value may still be set)
     * - Collected: key/value was partially collected, but not yet cleaned up
     * - Unset: marked as unset, awaiting cleanup or reuse
     */
    private interface ReferenceEntry<K : Any, V : Any> {
        /**
         * Returns the value reference from this entry.
         */
        /**
         * Sets the value reference for this entry.
         */
        var valueReference: ValueReference<K, V>?
            get() = throw UnsupportedOperationException()
            set(_) = throw UnsupportedOperationException()

        /**
         * Returns the next entry in the chain.
         */
        val next: ReferenceEntry<K, V>?
            get() = throw UnsupportedOperationException()

        /**
         * Returns the entry's hash.
         */
        val hash: Int
            get() = throw UnsupportedOperationException()

        /**
         * Returns the key for this entry.
         */
        val key: K
            get() = throw UnsupportedOperationException()
        /*
         * Used by entries that use access order. Access entries are maintained in a doubly-linked list.
         * New entries are added at the tail of the list at write time; stale entries are expired from
         * the head of the list.
         */
        /**
         * Returns the time that this entry was last accessed, in ns.
         */
        /**
         * Sets the entry access time in ns.
         */
        var accessTime: Long
            get() = throw UnsupportedOperationException()
            set(_) = throw UnsupportedOperationException()
        /**
         * Returns the next entry in the access queue.
         */
        /**
         * Sets the next entry in the access queue.
         */
        var nextInAccessQueue: ReferenceEntry<K, V>
            get() = throw UnsupportedOperationException()
            set(_) = throw UnsupportedOperationException()
        /**
         * Returns the previous entry in the access queue.
         */
        /**
         * Sets the previous entry in the access queue.
         */
        var previousInAccessQueue: ReferenceEntry<K, V>
            get() = throw UnsupportedOperationException()
            set(_) = throw UnsupportedOperationException()
        /*
         * Implemented by entries that use write order. Write entries are maintained in a
         * doubly-linked list. New entries are added at the tail of the list at write time and stale
         * entries are expired from the head of the list.
         */
        /**
         * Returns the time that this entry was last written, in ns.
         */
        /**
         * Sets the entry write time in ns.
         */
        var writeTime: Long
            get() = throw UnsupportedOperationException()
            set(_) = throw UnsupportedOperationException()
        /**
         * Returns the next entry in the write queue.
         */
        /**
         * Sets the next entry in the write queue.
         */
        var nextInWriteQueue: ReferenceEntry<K, V>
            get() = throw UnsupportedOperationException()
            set(_) = throw UnsupportedOperationException()
        /**
         * Returns the previous entry in the write queue.
         */
        /**
         * Sets the previous entry in the write queue.
         */
        var previousInWriteQueue: ReferenceEntry<K, V>
            get() = throw UnsupportedOperationException()
            set(_) = throw UnsupportedOperationException()
    }

    private object NullEntry : ReferenceEntry<Any, Any> {
        override var valueReference: ValueReference<Any, Any>?
            get() = null
            set(_) {}

        override val next: ReferenceEntry<Any, Any>?
            get() = null

        override val hash: Int
            get() = 0

        override val key: Any
            get() = Unit

        override var accessTime: Long
            get() = 0
            set(_) {}

        override var nextInAccessQueue: ReferenceEntry<Any, Any>
            get() = this
            set(_) {}

        override var previousInAccessQueue: ReferenceEntry<Any, Any>
            get() = this
            set(_) {}

        override var writeTime: Long
            get() = 0
            set(_) {}

        override var nextInWriteQueue: ReferenceEntry<Any, Any>
            get() = this
            set(_) {}

        override var previousInWriteQueue: ReferenceEntry<Any, Any>
            get() = this
            set(_) {}
    }

    /*
     * Note: All of this duplicate code sucks, but it saves a lot of memory. If only Java had mixins!
     * To maintain this code, make a change for the strong reference type. Then, cut and paste, and
     * replace "Strong" with "Soft" or "Weak" within the pasted text. The primary difference is that
     * strong entries store the key reference directly while soft and weak entries delegate to their
     * respective superclasses.
     */
    /**
     * Used for strongly-referenced keys.
     */
    private open class StrongEntry<K : Any, V : Any>(
        override val key: K, // The code below is exactly the same for each entry type.
        override val hash: Int,
        override val next: ReferenceEntry<K, V>?
    ) : ReferenceEntry<K, V> {

        private val _valueReference = atomic<ValueReference<K, V>?>(unset())
        override var valueReference: ValueReference<K, V>? = _valueReference.value
    }

    private class StrongAccessEntry<K : Any, V : Any>(
        key: K,
        hash: Int,
        next: ReferenceEntry<K, V>?
    ) :
        StrongEntry<K, V>(key, hash, next) {
        // The code below is exactly the same for each access entry type.

        private val _accessTime = atomic(Long.MAX_VALUE)
        override var accessTime = _accessTime.value

        // Guarded By Segment.this
        override var nextInAccessQueue: ReferenceEntry<K, V> = nullEntry()

        // Guarded By Segment.this
        override var previousInAccessQueue: ReferenceEntry<K, V> = nullEntry()
    }

    private class StrongWriteEntry<K : Any, V : Any>(
        key: K,
        hash: Int,
        next: ReferenceEntry<K, V>?
    ) :
        StrongEntry<K, V>(key, hash, next) {
        // The code below is exactly the same for each write entry type.
        private val _writeTime = atomic(Long.MAX_VALUE)
        override var writeTime = _writeTime.value

        // Guarded By Segment.this
        override var nextInWriteQueue: ReferenceEntry<K, V> = nullEntry()

        // Guarded By Segment.this
        override var previousInWriteQueue: ReferenceEntry<K, V> = nullEntry()
    }

    private class StrongAccessWriteEntry<K : Any, V : Any>(
        key: K,
        hash: Int,
        next: ReferenceEntry<K, V>?
    ) :
        StrongEntry<K, V>(key, hash, next) {
        // The code below is exactly the same for each access entry type.
        private val _accessTime = atomic(Long.MAX_VALUE)
        override var accessTime: Long = _accessTime.value

        // Guarded By Segment.this
        override var nextInAccessQueue: ReferenceEntry<K, V> = nullEntry()

        // Guarded By Segment.this
        override var previousInAccessQueue: ReferenceEntry<K, V> = nullEntry()

        // The code below is exactly the same for each write entry type.
        private val _writeTime = atomic(Long.MAX_VALUE)
        override var writeTime: Long = _writeTime.value

        // Guarded By Segment.this
        override var nextInWriteQueue: ReferenceEntry<K, V> = nullEntry()

        // Guarded By Segment.this
        override var previousInWriteQueue: ReferenceEntry<K, V> = nullEntry()
    }

    /**
     * References a strong value.
     */
    private open class StrongValueReference<K : Any, V : Any>(private val referent: V) :
        ValueReference<K, V> {
        override fun get(): V = referent
        override val weight: Int = 1
        override val entry: ReferenceEntry<K, V>? = null
        override fun copyFor(value: V?, entry: ReferenceEntry<K, V>?): ValueReference<K, V> = this
        override val isActive: Boolean = true
        override fun notifyNewValue(newValue: V) {}
    }

    /**
     * References a strong value.
     */
    private class WeightedStrongValueReference<K : Any, V : Any>(
        referent: V,
        override val weight: Int
    ) :
        StrongValueReference<K, V>(referent)

    /**
     * This method is a convenience for testing. Code should call [Segment.newEntry] directly.
     */

    private fun newEntry(key: K, hash: Int, next: ReferenceEntry<K, V>?): ReferenceEntry<K, V> {
        val segment = segmentFor(hash)
        segment.reentrantLock.lock()
        return try {
            segment.newEntry(key, hash, next)
        } finally {
            segment.reentrantLock.unlock()
        }
    }

    /**
     * This method is a convenience for testing. Code should call [Segment.copyEntry] directly.
     */
    // Guarded By Segment.this
    private fun copyEntry(
        original: ReferenceEntry<K, V>,
        newNext: ReferenceEntry<K, V>?
    ): ReferenceEntry<K, V>? {
        val hash = original.hash
        return segmentFor(hash).copyEntry(original, newNext)
    }

    /**
     * This method is a convenience for testing. Code should call [Segment.setValue] instead.
     */
    // Guarded By Segment.this
    private fun newValueReference(
        entry: ReferenceEntry<K, V>,
        value: V,
        weight: Int
    ): ValueReference<K, V> {
        val hash = entry.hash
        return valueStrength.referenceValue(segmentFor(hash), entry, value, weight)
    }

    private fun hash(key: K): Int = rehash(key.hashCode())

    /**
     * Returns the segment that should be used for a key with the given hash.
     *
     * @param hash the hash code for the key
     * @return the segment
     */
    private fun segmentFor(hash: Int): Segment<K, V> = // TODO(fry): Lazily create segments?
        segments[hash ushr segmentShift and segmentMask] as Segment<K, V>

    private fun createSegment(initialCapacity: Int, maxSegmentWeight: Long): Segment<K, V> =
        Segment(this, initialCapacity, maxSegmentWeight)
    // expiration
    /**
     * Returns true if the entry has expired.
     */
    private fun isExpired(entry: ReferenceEntry<K, V>, now: Long): Boolean =
        if (expiresAfterAccess && now - entry.accessTime >= expireAfterAccessNanos) {
            true
        } else {
            expiresAfterWrite && now - entry.writeTime >= expireAfterWriteNanos
        }

    // Inner Classes

    private class SegmentTable<K : Any, V : Any>(val size: Int) {
        private val table: AtomicArray<ReferenceEntry<K, V>?> = atomicArrayOfNulls(size)
        operator fun get(idx: Int) = table[idx].value
        operator fun set(idx: Int, value: ReferenceEntry<K, V>?) {
            table[idx].value = value
        }
    }

    /**
     * Segments are specialized versions of hash tables.
     */
    private class Segment<K : Any, V : Any>(
        private val map: LocalCache<K, V>,
        initialCapacity: Int,
        private val maxSegmentWeight: Long
    ) {
        /*
         * TODO(fry): Consider copying variables (like evictsBySize) from outer class into this class.
         * It will require more memory but will reduce indirection.
         */
        /*
         * Segments maintain a table of entry lists that are ALWAYS kept in a consistent state, so can
         * be read without locking. Next fields of nodes are immutable (final). All list additions are
         * performed at the front of each bin. This makes it easy to check changes, and also fast to
         * traverse. When nodes would otherwise be changed, new nodes are created to replace them. This
         * works well for hash tables since the bin lists tend to be short. (The average length is less
         * than two.)
         *
         * Read operations can thus proceed without locking, but rely on selected uses of volatiles to
         * ensure that completed write operations performed by other threads are noticed. For most
         * purposes, the "count" field, tracking the number of elements, serves as that volatile
         * variable ensuring visibility. This is convenient because this field needs to be read in many
         * read operations anyway:
         *
         * - All (unsynchronized) read operations must first read the "count" field, and should not
         * look at table entries if it is 0.
         *
         * - All (synchronized) write operations should write to the "count" field after structurally
         * changing any bin. The operations must not take any action that could even momentarily
         * cause a concurrent read operation to see inconsistent data. This is made easier by the
         * nature of the read operations in Map. For example, no operation can reveal that the table
         * has grown but the threshold has not yet been updated, so there are no atomicity requirements
         * for this with respect to reads.
         *
         * As a guide, all critical volatile reads and writes to the count field are marked in code
         * comments.
         */

        val reentrantLock = reentrantLock()

        /**
         * The number of live elements in this segment's region.
         */
        private val count = atomic(0)

        /**
         * The weight of the live elements in this segment's region.
         */
        private var totalWeight: Long = 0

        /**
         * Number of updates that alter the size of the table. This is used during bulk-read methods to
         * make sure they see a consistent snapshot: If modCounts change during a traversal of segments
         * loading size or checking containsValue, then we might have an inconsistent view of state
         * so (usually) must retry.
         */
        private var modCount = 0

        /**
         * The table is expanded when its size exceeds this threshold. (The value of this field is
         * always `(int) (capacity * 0.75)`.)
         */
        private var threshold = 0

        /**
         * The per-segment table.
         */
        private val table: AtomicRef<SegmentTable<K, V>>

        /**
         * The recency queue is used to record which entries were accessed for updating the access
         * list's ordering. It is drained as a batch operation when either the DRAIN_THRESHOLD is
         * crossed or a write occurs on the segment.
         */
        private val recencyQueue: Queue<ReferenceEntry<K, V>>

        /**
         * A counter of the number of reads since the last write, used to drain queues on a small
         * fraction of read operations.
         */
        private val readCount = atomic(0)

        /**
         * A queue of elements currently in the map, ordered by write time. Elements are added to the
         * tail of the queue on write.
         */
        private val writeQueue: MutableQueue<ReferenceEntry<K, V>>

        /**
         * A queue of elements currently in the map, ordered by access time. Elements are added to the
         * tail of the queue on access (note that writes count as accesses).
         */
        private val accessQueue: MutableQueue<ReferenceEntry<K, V>>

        fun newEntry(key: K, hash: Int, next: ReferenceEntry<K, V>?): ReferenceEntry<K, V> =
            map.entryFactory.newEntry(this, key, hash, next)

        /**
         * Copies `original` into a new entry chained to `newNext`. Returns the new entry,
         * or `null` if `original` was already garbage collected.
         */
        fun copyEntry(
            original: ReferenceEntry<K, V>,
            newNext: ReferenceEntry<K, V>?
        ): ReferenceEntry<K, V>? {
            val valueReference = original.valueReference
            val value = valueReference!!.get()
            if (value == null && valueReference.isActive) {
                // value collected
                return null
            }
            val newEntry = map.entryFactory.copyEntry(this, original, newNext)
            newEntry.valueReference = valueReference.copyFor(value, newEntry)
            return newEntry
        }

        /**
         * Sets a new value of an entry. Adds newly created entries at the end of the access queue.
         */
        fun setValue(entry: ReferenceEntry<K, V>, key: K, value: V, now: Long) {
            val previous = entry.valueReference
            val weight = map.weigher(key, value)
            if (weight < 0) throw IllegalStateException("Weights must be non-negative")
            entry.valueReference = map.valueStrength.referenceValue(this, entry, value, weight)
            recordWrite(entry, weight, now)
            previous?.notifyNewValue(value)
        }

        // recency queue, shared by expiration and eviction
        /**
         * Records the relative order in which this read was performed by adding `entry` to the
         * recency queue. At write-time, or when the queue is full past the threshold, the queue will
         * be drained and the entries therein processed.
         *
         *
         *
         * Note: locked reads should use [.recordLockedRead].
         */
        private fun recordRead(entry: ReferenceEntry<K, V>, now: Long) {
            if (map.recordsAccess) {
                entry.accessTime = now
            }
            recencyQueue.add(entry)
        }

        /**
         * Updates the eviction metadata that `entry` was just read. This currently amounts to
         * adding `entry` to relevant eviction lists.
         *
         *
         *
         * Note: this method should only be called under lock, as it directly manipulates the
         * eviction queues. Unlocked reads should use [.recordRead].
         */
        private fun recordLockedRead(entry: ReferenceEntry<K, V>, now: Long) {
            if (map.recordsAccess) {
                entry.accessTime = now
            }
            accessQueue.add(entry)
        }

        /**
         * Updates eviction metadata that `entry` was just written. This currently amounts to
         * adding `entry` to relevant eviction lists.
         */
        private fun recordWrite(entry: ReferenceEntry<K, V>, weight: Int, now: Long) {
            // we are already under lock, so drain the recency queue immediately
            drainRecencyQueue()
            totalWeight += weight.toLong()
            if (map.recordsAccess) {
                entry.accessTime = now
            }
            if (map.recordsWrite) {
                entry.writeTime = now
            }
            accessQueue.add(entry)
            writeQueue.add(entry)
        }

        /**
         * Drains the recency queue, updating eviction metadata that the entries therein were read in
         * the specified relative order. This currently amounts to adding them to relevant eviction
         * lists (accounting for the fact that they could have been removed from the map since being
         * added to the recency queue).
         */
        private fun drainRecencyQueue() {
            while (true) {
                val e = recencyQueue.poll() ?: break
                // An entry may be in the recency queue despite it being removed from
                // the map . This can occur when the entry was concurrently read while a
                // writer is removing it from the segment or after a clear has removed
                // all of the segment's entries.
                if (accessQueue.contains(e)) {
                    accessQueue.add(e)
                }
            }
        }
        // expiration
        /**
         * Cleanup expired entries when the lock is available.
         */
        private fun tryExpireEntries(now: Long) {
            if (reentrantLock.tryLock()) {
                try {
                    expireEntries(now)
                } finally {
                    reentrantLock.unlock()
                    // don't call postWriteCleanup as we're in a read
                }
            }
        }

        private fun expireEntries(now: Long) {
            drainRecencyQueue()
            while (true) {
                val e = writeQueue.peek()?.takeIf { map.isExpired(it, now) } ?: break
                if (!removeEntry(e, e.hash, RemovalCause.EXPIRED)) {
                    throw AssertionError()
                }
            }

            while (true) {
                val e = accessQueue.peek()?.takeIf { map.isExpired(it, now) } ?: break
                if (!removeEntry(e, e.hash, RemovalCause.EXPIRED)) {
                    throw AssertionError()
                }
            }
        }

        // eviction
        private fun enqueueNotification(entry: ReferenceEntry<K, V>, cause: RemovalCause?) {
            enqueueNotification(entry.key, entry.hash, entry.valueReference, cause)
        }

        private fun enqueueNotification(
            key: K?,
            hash: Int,
            valueReference: ValueReference<K, V>?,
            cause: RemovalCause?
        ) {
            valueReference?.weight?.toLong()?.apply {
                totalWeight -= this
            }
        }

        /**
         * Performs eviction if the segment is over capacity. Avoids flushing the entire cache if the
         * newest entry exceeds the maximum weight all on its own.
         *
         * @param newest the most recently added entry
         */
        private fun evictEntries(newest: ReferenceEntry<K, V>) {
            if (!map.evictsBySize) {
                return
            }
            drainRecencyQueue()

            // If the newest entry by itself is too heavy for the segment, don't bother evicting
            // anything else, just that
            if (newest.valueReference!!.weight > maxSegmentWeight) {
                if (!removeEntry(newest, newest.hash, RemovalCause.SIZE)) {
                    throw AssertionError()
                }
            }
            while (totalWeight > maxSegmentWeight) {
                val e = nextEvictable
                if (!removeEntry(e, e.hash, RemovalCause.SIZE)) {
                    throw AssertionError()
                }
            }
        }

        // TODO(fry): instead implement this with an eviction head

        private val nextEvictable: ReferenceEntry<K, V>
            get() {
                for (e in accessQueue) {
                    val weight = e.valueReference!!.weight
                    if (weight > 0) {
                        return e
                    }
                }
                throw AssertionError()
            }

        /**
         * Returns first entry of bin for given hash.
         */
        private fun getFirst(hash: Int): ReferenceEntry<K, V>? {
            // read this volatile field only once
            val table = table.value
            return table[hash and table.size - 1]
        }

        // Specialized implementations of map methods
        private fun getEntry(key: K, hash: Int): ReferenceEntry<K, V>? {
            var e = getFirst(hash)
            while (e != null) {
                if (e.hash != hash) {
                    e = e.next
                    continue
                }
                val entryKey = e.key
                if (key == entryKey) {
                    return e
                }
                e = e.next
            }
            return null
        }

        private fun getLiveEntry(key: K, hash: Int, now: Long): ReferenceEntry<K, V>? {
            val e = getEntry(key, hash)
            if (e == null) {
                return null
            } else if (map.isExpired(e, now)) {
                tryExpireEntries(now)
                return null
            }
            return e
        }

        /**
         * Gets the value from an entry. Returns null if the entry is invalid, partially-collected,
         * loading, or expired.
         */

        fun get(key: K, hash: Int): V? {
            return try {
                if (count.value != 0) { // read-volatile
                    val now = map.ticker()
                    val e = getLiveEntry(key, hash, now) ?: return null
                    val value = e.valueReference?.get()
                    if (value != null) {
                        recordRead(e, now)
                        return value
                    }
                }
                null
            } finally {
                postReadCleanup()
            }
        }

        fun getOrPut(key: K, hash: Int, defaultValue: () -> V): V {
            reentrantLock.lock()
            return try {
                if (count.value != 0) { // read-volatile
                    val now = map.ticker()
                    val e = getLiveEntry(key, hash, now)
                    val value = e?.valueReference?.get()
                    if (value != null) {
                        recordRead(e, now)
                        return value
                    }
                }
                val default = defaultValue()
                put(key, hash, default, false)
                default
            } finally {
                reentrantLock.unlock()
                postReadCleanup()
            }
        }

        fun put(key: K, hash: Int, value: V, onlyIfAbsent: Boolean): V? {
            reentrantLock.lock()
            return try {
                val now = map.ticker()
                preWriteCleanup(now)
                if (count.value + 1 > threshold) { // ensure capacity
                    expand()
                }
                val table = table.value
                val index = hash and table.size - 1
                val first = table[index]

                // Look for an existing entry.
                var e: ReferenceEntry<K, V>? = first
                while (e != null) {
                    val entryKey = e.key
                    if (e.hash == hash && key == entryKey) {
                        // We found an existing entry.
                        val valueReference = e.valueReference
                        val entryValue = valueReference!!.get()
                        return when {
                            entryValue == null -> {
                                ++modCount
                                val newCount = if (valueReference.isActive) {
                                    enqueueNotification(
                                        key,
                                        hash,
                                        valueReference,
                                        RemovalCause.COLLECTED
                                    )
                                    setValue(e, key, value, now)
                                    count.value // count remains unchanged
                                } else {
                                    setValue(e, key, value, now)
                                    count.value + 1
                                }
                                count.value = newCount // write-volatile
                                evictEntries(e)
                                null
                            }

                            onlyIfAbsent -> {
                                // Mimic
                                // "if (!map.containsKey(key)) ...
                                // else return map.get(key);
                                recordLockedRead(e, now)
                                entryValue
                            }

                            else -> {
                                // clobber existing entry, count remains unchanged
                                ++modCount
                                enqueueNotification(
                                    key,
                                    hash,
                                    valueReference,
                                    RemovalCause.REPLACED
                                )
                                setValue(e, key, value, now)
                                evictEntries(e)
                                entryValue
                            }
                        }
                    }
                    e = e.next
                }

                // Create a new entry.
                ++modCount
                val newEntry = newEntry(key, hash, first)
                setValue(newEntry, key, value, now)
                table[index] = newEntry
                count.plusAssign(1)
                evictEntries(newEntry)
                null
            } finally {
                reentrantLock.unlock()
                postWriteCleanup()
            }
        }

        fun remove(key: K, hash: Int): V? {
            reentrantLock.lock()
            return try {
                val now = map.ticker()
                preWriteCleanup(now)
                val table = table.value
                val index = hash and table.size - 1
                val first = table[index]
                var e = first
                while (e != null) {
                    val entryKey = e.key
                    if (e.hash == hash && key == entryKey) {
                        val valueReference = e.valueReference
                        val entryValue = valueReference!!.get()
                        val cause: RemovalCause = when {
                            entryValue != null -> {
                                RemovalCause.EXPLICIT
                            }

                            valueReference.isActive -> {
                                RemovalCause.COLLECTED
                            }

                            else -> {
                                // currently loading
                                return null
                            }
                        }
                        ++modCount
                        val newFirst = removeValueFromChain(
                            first!!, e, entryKey, hash, valueReference, cause
                        )
                        val newCount = count.value - 1
                        table[index] = newFirst
                        count.value = newCount // write-volatile
                        return entryValue
                    }
                    e = e.next
                }
                null
            } finally {
                reentrantLock.unlock()
                postWriteCleanup()
            }
        }

        fun clear() {
            if (count.value != 0) { // read-volatile
                reentrantLock.lock()
                try {
                    val table = table.value
                    for (i in 0 until table.size) {
                        var e = table[i]
                        while (e != null) {
                            // Loading references aren't actually in the map yet.
                            if (e.valueReference!!.isActive) {
                                enqueueNotification(e, RemovalCause.EXPLICIT)
                            }
                            e = e.next
                        }
                    }
                    for (i in 0 until table.size) {
                        table[i] = null
                    }
                    writeQueue.clear()
                    accessQueue.clear()
                    readCount.value = 0
                    ++modCount
                    count.value = 0 // write-volatile
                } finally {
                    reentrantLock.unlock()
                    postWriteCleanup()
                }
            }
        }

        /**
         * Expands the table if possible.
         */
        private fun expand() {
            val oldTable = table.value
            val oldCapacity = oldTable.size
            if (oldCapacity >= MAXIMUM_CAPACITY) {
                return
            }

            /*
             * Reclassify nodes in each list to new Map. Because we are using power-of-two expansion, the
             * elements from each bin must either stay at same index, or move with a power of two offset.
             * We eliminate unnecessary node creation by catching cases where old nodes can be reused
             * because their next fields won't change. Statistically, at the default threshold, only
             * about one-sixth of them need cloning when a table doubles. The nodes they replace will be
             * garbage collectable as soon as they are no longer referenced by any reader thread that may
             * be in the midst of traversing table right now.
             */
            var newCount = count.value
            val newTable = SegmentTable<K, V>(oldCapacity shl 1)
            threshold = newTable.size * 3 / 4
            val newMask = newTable.size - 1
            for (oldIndex in 0 until oldCapacity) {
                // We need to guarantee that any existing reads of old Map can
                // proceed. So we cannot yet null out each bin.
                val head = oldTable[oldIndex] ?: continue

                val next = head.next
                val headIndex = head.hash and newMask

                // Single node on list
                if (next == null) {
                    newTable[headIndex] = head
                } else {
                    // Reuse the consecutive sequence of nodes with the same target
                    // index from the end of the list. tail points to the first
                    // entry in the reusable list.
                    var tail = head
                    var tailIndex = headIndex
                    var entry = next
                    while (entry != null) {
                        val newIndex = entry.hash and newMask
                        if (newIndex != tailIndex) {
                            // The index changed. We'll need to copy the previous entry.
                            tailIndex = newIndex
                            tail = entry
                        }
                        entry = entry.next
                    }
                    newTable[tailIndex] = tail

                    // Clone nodes leading up to the tail.
                    var headEntry = head
                    while (headEntry !== tail) {
                        val newIndex = headEntry.hash and newMask
                        val newNext = newTable[newIndex]
                        val newFirst = copyEntry(headEntry, newNext)
                        if (newFirst != null) {
                            newTable[newIndex] = newFirst
                        } else {
                            removeCollectedEntry(headEntry)
                            newCount--
                        }
                        headEntry = headEntry.next ?: break
                    }
                }
            }
            table.value = newTable
            count.value = newCount
        }

        private fun removeValueFromChain(
            first: ReferenceEntry<K, V>,
            entry: ReferenceEntry<K, V>,
            key: K,
            hash: Int,
            valueReference: ValueReference<K, V>,
            cause: RemovalCause?
        ): ReferenceEntry<K, V>? {
            enqueueNotification(key, hash, valueReference, cause)
            writeQueue.remove(entry)
            accessQueue.remove(entry)
            return removeEntryFromChain(first, entry)
        }

        private fun removeEntryFromChain(
            first: ReferenceEntry<K, V>,
            entry: ReferenceEntry<K, V>
        ): ReferenceEntry<K, V>? {
            var newCount = count.value
            var newFirst = entry.next
            var e = first
            while (e !== entry) {
                val next = copyEntry(e, newFirst)
                if (next != null) {
                    newFirst = next
                } else {
                    removeCollectedEntry(e)
                    newCount--
                }
                e = e.next ?: break
            }
            count.value = newCount
            return newFirst
        }

        private fun removeCollectedEntry(entry: ReferenceEntry<K, V>) {
            enqueueNotification(entry, RemovalCause.COLLECTED)
            writeQueue.remove(entry)
            accessQueue.remove(entry)
        }

        private fun removeEntry(
            entry: ReferenceEntry<K, V>,
            hash: Int,
            cause: RemovalCause?
        ): Boolean {
            val table = table.value
            val index = hash and table.size - 1
            val first = table[index]
            var e = first

            while (e != null) {
                if (e === entry) {
                    ++modCount
                    val newFirst = removeValueFromChain(
                        first!!, e, e.key, hash, e.valueReference!!, cause
                    )
                    val newCount = count.value - 1
                    table[index] = newFirst
                    count.value = newCount // write-volatile
                    return true
                }
                e = e.next
            }
            return false
        }

        /**
         * Performs routine cleanup following a read. Normally cleanup happens during writes. If cleanup
         * is not observed after a sufficient number of reads, try cleaning up from the read thread.
         */
        private fun postReadCleanup() {
            if (readCount.incrementAndGet() and DRAIN_THRESHOLD == 0) {
                cleanUp()
            }
        }

        /**
         * Performs routine cleanup prior to executing a write. This should be called every time a
         * write thread acquires the segment lock, immediately after acquiring the lock.
         *
         *
         *
         * Post-condition: expireEntries has been run.
         */
        private fun preWriteCleanup(now: Long) {
            runLockedCleanup(now)
        }

        /**
         * Performs routine cleanup following a write.
         */
        private fun postWriteCleanup() {
            runUnlockedCleanup()
        }

        fun cleanUp() {
            val now = map.ticker()
            runLockedCleanup(now)
            runUnlockedCleanup()
        }

        private fun runLockedCleanup(now: Long) {
            if (reentrantLock.tryLock()) {
                try {
                    expireEntries(now) // calls drainRecencyQueue
                    readCount.value = 0
                } finally {
                    reentrantLock.unlock()
                }
            }
        }

        private fun runUnlockedCleanup() {
            // locked cleanup may generate notifications we can send unlocked
            /*if (!isHeldByCurrentThread) {
                map.processPendingNotifications()
            }*/
        }

        fun activeEntries(): Map<K, V> {
            if (count.value != 0) { // read-volatile
                reentrantLock.lock()
                try {
                    return buildMap {
                        val table = table.value
                        for (i in 0 until table.size) {
                            var e = table[i]
                            while (e != null) {
                                if (e.valueReference!!.isActive) {
                                    put(e.key, e.valueReference!!.get()!!)
                                }
                                e = e.next
                            }
                        }
                    }
                } finally {
                    reentrantLock.unlock()
                }
            }
            return emptyMap()
        }

        init {
            threshold = initialCapacity * 3 / 4 // 0.75
            if (!map.customWeigher && threshold.toLong() == maxSegmentWeight) {
                // prevent spurious expansion before eviction
                threshold++
            }
            table = atomic(SegmentTable(initialCapacity))
            recencyQueue = if (map.usesAccessQueue) AtomicLinkedQueue() else discardingQueue()
            writeQueue = if (map.usesWriteQueue) WriteQueue() else discardingQueue()
            accessQueue = if (map.usesAccessQueue) AccessQueue() else discardingQueue()
        }
    }
    // Queues

    private interface Queue<T : Any> {
        fun poll(): T?
        fun add(value: T)
    }

    private interface MutableQueue<E : Any> : Queue<E>, Iterable<E> {
        fun peek(): E?
        fun isEmpty(): Boolean
        val size: Int
        fun clear()
        fun remove(element: E): Boolean
        fun contains(element: E): Boolean
    }

    private class AtomicLinkedQueue<T : Any> : Queue<T> {
        private val head: AtomicRef<Node<T?>> = atomic(Node(null))
        private val tail: AtomicRef<Node<T?>> = atomic(head.value)

        private class Node<T>(val value: T) {
            val next = atomic<Node<T>?>(null)
        }

        override fun add(value: T) {
            val node: Node<T?> = Node(value)
            tail.loop { curTail ->
                val curNext = curTail.next.value
                if (curNext != null) {
                    tail.compareAndSet(curTail, curNext)
                    return@loop
                }
                if (curTail.next.compareAndSet(null, node)) {
                    tail.compareAndSet(curTail, node)
                    return
                }
            }
        }

        override fun poll(): T? {
            head.loop { curHead ->
                val next = curHead.next.value ?: return null
                if (head.compareAndSet(curHead, next)) return next.value
            }
        }
    }

    /**
     * A custom queue for managing eviction order. Note that this is tightly integrated with `ReferenceEntry`, upon which it relies to perform its linking.
     *
     *
     *
     * Note that this entire implementation makes the assumption that all elements which are in
     * the map are also in this queue, and that all elements not in the queue are not in the map.
     *
     *
     *
     * The benefits of creating our own queue are that (1) we can replace elements in the middle
     * of the queue as part of copyWriteEntry, and (2) the contains method is highly optimized
     * for the current model.
     */

    private class WriteQueue<K : Any, V : Any> : MutableQueue<ReferenceEntry<K, V>> {
        private val head: ReferenceEntry<K, V> = object : ReferenceEntry<K, V> {
            override var writeTime: Long
                get() = Long.MAX_VALUE
                set(_) {}
            override var nextInWriteQueue: ReferenceEntry<K, V> = this
            override var previousInWriteQueue: ReferenceEntry<K, V> = this
        }

        // implements Queue
        override fun add(value: ReferenceEntry<K, V>) {
            // unlink
            connectWriteOrder(value.previousInWriteQueue, value.nextInWriteQueue)

            // add to tail
            connectWriteOrder(head.previousInWriteQueue, value)
            connectWriteOrder(value, head)
        }

        override fun peek(): ReferenceEntry<K, V>? {
            val next = head.nextInWriteQueue
            return if (next === head) null else next
        }

        override fun poll(): ReferenceEntry<K, V>? {
            val next = head.nextInWriteQueue
            if (next === head) {
                return null
            }
            remove(next)
            return next
        }

        override fun remove(element: ReferenceEntry<K, V>): Boolean {
            val previous = element.previousInWriteQueue
            val next = element.nextInWriteQueue
            connectWriteOrder(previous, next)
            nullifyWriteOrder(element)
            return next !== NullEntry
        }

        override fun contains(element: ReferenceEntry<K, V>): Boolean =
            element.nextInWriteQueue !== NullEntry

        override fun isEmpty(): Boolean =
            head.nextInWriteQueue === head

        override val size: Int
            get() {
                var size = 0
                var e = head.nextInWriteQueue
                while (e !== head) {
                    size++
                    e = e.nextInWriteQueue
                }
                return size
            }

        override fun clear() {
            var e = head.nextInWriteQueue
            while (e !== head) {
                val next = e.nextInWriteQueue
                nullifyWriteOrder(e)
                e = next
            }
            head.nextInWriteQueue = head
            head.previousInWriteQueue = head
        }

        override fun iterator(): Iterator<ReferenceEntry<K, V>> = iterator {
            var value = peek()
            while (value != null) {
                yield(value)
                val next = value.nextInWriteQueue
                value = if (next === head) null else next
            }
        }
    }

    /**
     * A custom queue for managing access order. Note that this is tightly integrated with
     * `ReferenceEntry`, upon which it reliese to perform its linking.
     *
     *
     *
     * Note that this entire implementation makes the assumption that all elements which are in
     * the map are also in this queue, and that all elements not in the queue are not in the map.
     *
     *
     *
     * The benefits of creating our own queue are that (1) we can replace elements in the middle
     * of the queue as part of copyWriteEntry, and (2) the contains method is highly optimized
     * for the current model.
     */
    private class AccessQueue<K : Any, V : Any> : MutableQueue<ReferenceEntry<K, V>> {
        private val head: ReferenceEntry<K, V> = object : ReferenceEntry<K, V> {
            override var accessTime: Long
                get() = Long.MAX_VALUE
                set(_) {}
            override var nextInAccessQueue: ReferenceEntry<K, V> = this
            override var previousInAccessQueue: ReferenceEntry<K, V> = this
        }

        // implements Queue
        override fun add(value: ReferenceEntry<K, V>) {
            // unlink
            connectAccessOrder(value.previousInAccessQueue, value.nextInAccessQueue)

            // add to tail
            connectAccessOrder(head.previousInAccessQueue, value)
            connectAccessOrder(value, head)
        }

        override fun peek(): ReferenceEntry<K, V>? {
            val next = head.nextInAccessQueue
            return if (next === head) null else next
        }

        override fun poll(): ReferenceEntry<K, V>? {
            val next = head.nextInAccessQueue
            if (next === head) {
                return null
            }
            remove(next)
            return next
        }

        override fun remove(element: ReferenceEntry<K, V>): Boolean {
            val previous = element.previousInAccessQueue
            val next = element.nextInAccessQueue
            connectAccessOrder(previous, next)
            nullifyAccessOrder(element)
            return next !== NullEntry
        }

        override fun contains(element: ReferenceEntry<K, V>): Boolean =
            element.nextInAccessQueue !== NullEntry

        override fun isEmpty(): Boolean =
            head.nextInAccessQueue === head

        override val size: Int
            get() {
                var size = 0
                var e = head.nextInAccessQueue
                while (e !== head) {
                    size++
                    e = e.nextInAccessQueue
                }
                return size
            }

        override fun clear() {
            var e = head.nextInAccessQueue
            while (e !== head) {
                val next = e.nextInAccessQueue
                nullifyAccessOrder(e)
                e = next
            }
            head.nextInAccessQueue = head
            head.previousInAccessQueue = head
        }

        override fun iterator(): Iterator<ReferenceEntry<K, V>> = iterator {
            var value = peek()
            while (value != null) {
                yield(value)
                val next = value.nextInAccessQueue
                value = if (next === head) null else next
            }
        }
    }

    // Cache support
    fun cleanUp() {
        for (segment in segments) {
            segment?.cleanUp()
        }
    }

    // ConcurrentMap methods
    fun getIfPresent(key: K): V? {
        val hash = hash(key)
        return segmentFor(hash).get(key, hash)
    }

    fun put(key: K, value: V): V? {
        val hash = hash(key)
        return segmentFor(hash).put(key, hash, value, false)
    }

    fun getOrPut(key: K, defaultValue: () -> V): V {
        val hash = hash(key)
        return segmentFor(hash).getOrPut(key, hash, defaultValue)
    }

    fun clear() {
        for (segment in segments) {
            segment?.clear()
        }
    }

    fun remove(key: K): V? {
        val hash = hash(key)
        return segmentFor(hash).remove(key, hash)
    }

    fun getAllPresent(): Map<K, V> {
        return buildMap {
            for (segment in segments) {
                segment?.let { putAll(it.activeEntries()) }
            }
        }
    }

    // Serialization Support
    internal class LocalManualCache<K : Any, V : Any> private constructor(private val localCache: LocalCache<K, V>) :
        Cache<K, V> {
        constructor(builder: CacheBuilder<K, V>) : this(LocalCache<K, V>(builder))

        // Cache methods
        override fun getIfPresent(key: K): V? {
            return localCache.getIfPresent(key)
        }

        override fun put(key: K, value: V) {
            localCache.put(key, value)
        }

        override fun invalidate(key: K) {
            localCache.remove(key)
        }

        override fun getOrPut(key: K, valueProducer: () -> V): V {
            return localCache.getOrPut(key, valueProducer)
        }

        override fun getAllPresent(keys: List<*>): Map<K, V> {
            return localCache.getAllPresent().filterKeys { it in keys }
        }

        override fun getAllPresent(): Map<K, V> {
            return localCache.getAllPresent()
        }

        override fun invalidateAll(keys: List<K>) {
            TODO("Not yet implemented")
        }

        override fun putAll(map: Map<K, V>) {
            TODO("Not yet implemented")
        }

        override fun invalidateAll() {
            localCache.clear()
        }

        override fun size(): Long {
            TODO("Not yet implemented")
        }
    }

    companion object {
        /*
         * The basic strategy is to subdivide the table among Segments, each of which itself is a
         * concurrently readable hash table. The map supports non-blocking reads and concurrent writes
         * across different segments.
         *
         * If a maximum size is specified, a best-effort bounding is performed per segment, using a
         * page-replacement algorithm to determine which entries to evict when the capacity has been
         * exceeded.
         *
         * The page replacement algorithm's data structures are kept casually consistent with the map. The
         * ordering of writes to a segment is sequentially consistent. An update to the map and recording
         * of reads may not be immediately reflected on the algorithm's data structures. These structures
         * are guarded by a lock and operations are applied in batches to avoid lock contention. The
         * penalty of applying the batches is spread across threads so that the amortized cost is slightly
         * higher than performing just the operation without enforcing the capacity constraint.
         *
         * This implementation uses a per-segment queue to record a memento of the additions, removals,
         * and accesses that were performed on the map. The queue is drained on writes and when it exceeds
         * its capacity threshold.
         *
         * The Least Recently Used page replacement algorithm was chosen due to its simplicity, high hit
         * rate, and ability to be implemented with O(1) time complexity. The initial LRU implementation
         * operates per-segment rather than globally for increased implementation simplicity. We expect
         * the cache hit rate to be similar to that of a global LRU algorithm.
         */
        // Constants
        private val OneWeigher: Weigher<Any, Any> = { _, _ -> 1 }

        /**
         * The maximum capacity, used if a higher value is implicitly specified by either of the
         * constructors with arguments. MUST be a power of two <= 1<<30 to ensure that entries are
         * indexable using ints.
         */
        const val MAXIMUM_CAPACITY = 1 shl 30

        /**
         * The maximum number of segments to allow; used to bound constructor arguments.
         */
        const val MAX_SEGMENTS = 1 shl 16 // slightly conservative

        /**
         * Number of cache access operations that can be buffered per segment before the cache's recency
         * ordering information is updated. This is used to avoid lock contention by recording a memento
         * of reads and delaying a lock acquisition until the threshold is crossed or a mutation occurs.
         *
         *
         *
         * This must be a (2^n)-1 as it is used as a mask.
         */
        const val DRAIN_THRESHOLD = 0x3F

        /**
         * Placeholder. Indicates that the value hasn't been set yet.
         */
        private val UNSET: ValueReference<Any, Any> = object : ValueReference<Any, Any> {
            override fun get(): Any? {
                return null
            }

            override val weight: Int
                get() = 0
            override val entry: ReferenceEntry<Any, Any>?
                get() = null

            override fun copyFor(
                value: Any?,
                entry: ReferenceEntry<Any, Any>?
            ): ValueReference<Any, Any> {
                return this
            }

            override val isActive: Boolean
                get() = false

            override fun notifyNewValue(newValue: Any) {}
        }

        /**
         * Singleton placeholder that indicates a value is being loaded.
         */
        @Suppress("UNCHECKED_CAST")
        private fun <K : Any, V : Any> unset() = UNSET as ValueReference<K, V>

        @Suppress("UNCHECKED_CAST")
        private fun <K : Any, V : Any> nullEntry() = NullEntry as ReferenceEntry<K, V>

        private val DISCARDING_QUEUE: MutableQueue<Any> = object : MutableQueue<Any> {
            override fun add(value: Any) {}

            override fun peek(): Any? = null

            override fun poll(): Any? = null

            override fun iterator(): MutableIterator<Any> = HashSet<Any>().iterator()

            override val size: Int = 0

            override fun isEmpty(): Boolean = true

            override fun clear() {}

            override fun remove(element: Any): Boolean = false

            override fun contains(element: Any): Boolean = false
        }

        /**
         * Queue that discards all elements.
         */
        @Suppress("UNCHECKED_CAST")
        private fun <E : Any> discardingQueue(): MutableQueue<E> =
            DISCARDING_QUEUE as MutableQueue<E>

        /**
         * Applies a supplemental hash function to a given hash code, which defends against poor quality
         * hash functions. This is critical when the concurrent hash map uses power-of-two length hash
         * tables, that otherwise encounter collisions for hash codes that do not differ in lower or
         * upper bits.
         *
         * @param hash hash code
         */
        fun rehash(hash: Int): Int {
            // Spread bits to regularize both segment and index locations,
            // using variant of single-word Wang/Jenkins hash.
            // TODO(kevinb): use Hashing/move this to Hashing?
            var h = hash
            h += h shl 15 xor -0x3283
            h = h xor (h ushr 10)
            h += h shl 3
            h = h xor (h ushr 6)
            h += (h shl 2) + (h shl 14)
            return h xor (h ushr 16)
        }

        // queues
        // Guarded By Segment.this
        private fun <K : Any, V : Any> connectAccessOrder(
            previous: ReferenceEntry<K, V>,
            next: ReferenceEntry<K, V>
        ) {
            previous.nextInAccessQueue = next
            next.previousInAccessQueue = previous
        }

        // Guarded By Segment.this
        private fun <K : Any, V : Any> nullifyAccessOrder(nulled: ReferenceEntry<K, V>) {
            val nullEntry: ReferenceEntry<K, V> = nullEntry()
            nulled.nextInAccessQueue = nullEntry
            nulled.previousInAccessQueue = nullEntry
        }

        // Guarded By Segment.this
        private fun <K : Any, V : Any> connectWriteOrder(
            previous: ReferenceEntry<K, V>,
            next: ReferenceEntry<K, V>
        ) {
            previous.nextInWriteQueue = next
            next.previousInWriteQueue = previous
        }

        // Guarded By Segment.this
        private fun <K : Any, V : Any> nullifyWriteOrder(nulled: ReferenceEntry<K, V>) {
            val nullEntry: ReferenceEntry<K, V> = nullEntry()
            nulled.nextInWriteQueue = nullEntry
            nulled.previousInWriteQueue = nullEntry
        }
    }

    /**
     * Creates a new, empty map with the specified strategy, initial capacity and concurrency level.
     */
    init {
        this.maxWeight = when {
            builder.expireAfterAccess == Duration.ZERO || builder.expireAfterWrite == Duration.ZERO -> 0L
            builder.weigher != null -> builder.maximumWeight
            else -> builder.maximumSize
        }
        this.weigher = builder.weigher ?: OneWeigher as Weigher<K, V>

        this.expireAfterAccessNanos =
            (if (builder.expireAfterAccess == Duration.INFINITE) Duration.ZERO else builder.expireAfterAccess)
                .inWholeNanoseconds

        this.expireAfterWriteNanos =
            (if (builder.expireAfterWrite == Duration.INFINITE) Duration.ZERO else builder.expireAfterWrite)
                .inWholeNanoseconds

        this.ticker = if (recordsTime) (builder.ticker ?: MonotonicTicker) else ({ 0L })
        this.entryFactory = EntryFactory.getFactory(usesAccessEntries, usesWriteEntries)
        var initialCapacity = builder.initialCapacity.coerceAtMost(MAXIMUM_CAPACITY)
        if (evictsBySize && !customWeigher) {
            initialCapacity = min(initialCapacity, maxWeight.toInt())
        }
        val concurrencyLevel = builder.concurrencyLevel.coerceAtMost(MAX_SEGMENTS)
        // Find the lowest power-of-two segmentCount that exceeds concurrencyLevel, unless
        // maximumSize/Weight is specified in which case ensure that each segment gets at least 10
        // entries. The special casing for size-based eviction is only necessary because that eviction
        // happens per segment instead of globally, so too many segments compared to the maximum size
        // will result in random eviction behavior.
        var segmentShift = 0
        var segmentCount = 1
        while (segmentCount < concurrencyLevel && (!evictsBySize || segmentCount * 20 <= maxWeight)) {
            ++segmentShift
            segmentCount = segmentCount shl 1
        }
        this.segmentShift = 32 - segmentShift
        segmentMask = segmentCount - 1
        segments = arrayOfNulls(segmentCount)
        var segmentCapacity = initialCapacity / segmentCount
        if (segmentCapacity * segmentCount < initialCapacity) {
            ++segmentCapacity
        }
        var segmentSize = 1
        while (segmentSize < segmentCapacity) {
            segmentSize = segmentSize shl 1
        }
        if (evictsBySize) {
            // Ensure sum of segment max weights = overall max weights
            var maxSegmentWeight = maxWeight / segmentCount + 1
            val remainder = maxWeight % segmentCount
            for (i in segments.indices) {
                if (i.toLong() == remainder) {
                    maxSegmentWeight--
                }
                segments[i] = createSegment(segmentSize, maxSegmentWeight)
            }
        } else {
            for (i in segments.indices) {
                segments[i] = createSegment(segmentSize, -1)
            }
        }
    }
}
