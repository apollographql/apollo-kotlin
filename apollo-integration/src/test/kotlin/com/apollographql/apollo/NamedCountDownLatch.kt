package com.apollographql.apollo

import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

/**
 * [java.util.concurrent.CountDownLatch] with an associated name for debugging
 */
class NamedCountDownLatch
/**
 * Constructs a `CountDownLatch` initialized with the given count.
 *
 * @param count the number of times [.countDown] must be invoked before threads can pass through [.await]
 * @throws IllegalArgumentException if `count` is negative
 */(private val name: String, count: Int) : CountDownLatch(count) {
  fun name(): String {
    return name
  }

  /**
   * Waits until latch countdown goes to zero. If timeout expires before latch count has gone to zero,
   * then a [TimeoutException] will be thrown.
   */
  @Throws(InterruptedException::class, TimeoutException::class)
  fun awaitOrThrowWithTimeout(timeout: Long, timeUnit: TimeUnit?) {
    if (!this.await(timeout, timeUnit)) {
      throw TimeoutException("Time expired before latch, " + name() + " count went to zero.")
    }
  }
}