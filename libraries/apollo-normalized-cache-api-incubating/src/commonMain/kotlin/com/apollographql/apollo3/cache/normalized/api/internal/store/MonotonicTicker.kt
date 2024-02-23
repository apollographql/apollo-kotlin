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

import kotlin.time.ExperimentalTime
import kotlin.time.TimeSource

@OptIn(ExperimentalTime::class)
internal val MonotonicTicker: Ticker = TimeSource.Monotonic.markNow().let { timeMark -> { timeMark.elapsedNow().inWholeNanoseconds } }
