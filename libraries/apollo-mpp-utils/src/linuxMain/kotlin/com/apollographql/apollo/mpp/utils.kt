package com.apollographql.apollo.mpp

import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
actual fun currentTimeMillis(): Long = Clock.System.now().epochSeconds
