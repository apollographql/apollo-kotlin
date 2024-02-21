package org.mobilenativefoundation.store.cache5

import kotlin.time.ExperimentalTime
import kotlin.time.TimeSource

@OptIn(ExperimentalTime::class)
internal val MonotonicTicker: Ticker = TimeSource.Monotonic.markNow().let { timeMark -> { timeMark.elapsedNow().inWholeNanoseconds } }
