package com.apollographql.apollo.internal.batch

import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit

class PeriodicJobSchedulerImpl : PeriodicJobScheduler {

  private val scheduledExecutorService: ScheduledExecutorService = Executors.newSingleThreadScheduledExecutor()
  private var pollDisposable: ScheduledFuture<*>? = null

  override fun schedulePeriodicJob(initialDelay: Long, interval: Long, unit: TimeUnit, job: () -> Unit) {
    pollDisposable = scheduledExecutorService.scheduleAtFixedRate({
      job()
    }, initialDelay, interval, unit)
  }

  override fun cancel() {
    pollDisposable?.cancel(true)
    pollDisposable = null
  }

  override fun isRunning(): Boolean {
    return pollDisposable != null
  }
}