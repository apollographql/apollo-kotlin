package com.apollographql.apollo.internal.batch

import java.util.concurrent.TimeUnit

interface PeriodicJobScheduler {
  fun schedulePeriodicJob(initialDelay: Long, interval: Long, unit: TimeUnit, job: () -> Unit)
  fun cancel()
  fun isRunning(): Boolean
}