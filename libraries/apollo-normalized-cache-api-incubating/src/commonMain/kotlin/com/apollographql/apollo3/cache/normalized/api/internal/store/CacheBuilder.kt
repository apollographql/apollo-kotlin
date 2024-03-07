/*
 * Taken from the Mobile Native Foundation Store project.
 * https://github.com/MobileNativeFoundation/Store/commit/e25e3c130187d9294ad5b998136b0498bd91d88f
 *
 * Copyright (c) 2017 The New York Times Company
 *
 * Copyright (c) 2010 The Guava Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this library except in
 * compliance with the License. You may obtain a copy of the License at
 *
 * www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific
 * language governing permissions and limitations under the License.
 */
package com.apollographql.apollo3.cache.normalized.api.internal.store

import kotlin.time.Duration

class CacheBuilder<Key : Any, Output : Any> {
    internal var concurrencyLevel = 4
        private set
    internal val initialCapacity = 16
    internal var maximumSize = UNSET
        private set
    internal var maximumWeight = UNSET
        private set
    internal var expireAfterAccess: Duration = Duration.INFINITE
        private set
    internal var expireAfterWrite: Duration = Duration.INFINITE
        private set
    internal var weigher: Weigher<Key, Output>? = null
        private set
    internal var ticker: Ticker? = null
        private set

    fun concurrencyLevel(producer: () -> Int): CacheBuilder<Key, Output> = apply {
        concurrencyLevel = producer.invoke()
    }

    fun maximumSize(maximumSize: Long): CacheBuilder<Key, Output> = apply {
        if (maximumSize < 0) {
            throw IllegalArgumentException("Maximum size must be non-negative.")
        }
        this.maximumSize = maximumSize
    }

    fun expireAfterAccess(duration: Duration): CacheBuilder<Key, Output> = apply {
        if (duration.isNegative()) {
            throw IllegalArgumentException("Duration must be non-negative.")
        }
        expireAfterAccess = duration
    }

    fun expireAfterWrite(duration: Duration): CacheBuilder<Key, Output> = apply {
        if (duration.isNegative()) {
            throw IllegalArgumentException("Duration must be non-negative.")
        }
        expireAfterWrite = duration
    }

    fun ticker(ticker: Ticker): CacheBuilder<Key, Output> = apply {
        this.ticker = ticker
    }

    fun weigher(maximumWeight: Long, weigher: Weigher<Key, Output>): CacheBuilder<Key, Output> = apply {
        if (maximumWeight < 0) {
            throw IllegalArgumentException("Maximum weight must be non-negative.")
        }

        this.maximumWeight = maximumWeight
        this.weigher = weigher
    }

    fun build(): Cache<Key, Output> {
        if (maximumSize != -1L && weigher != null) {
            throw IllegalStateException("Maximum size cannot be combined with weigher.")
        }
        return LocalCache.LocalManualCache(this)
    }

    companion object {
        private const val UNSET = -1L
    }
}
