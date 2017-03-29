package com.apollographql.apollo;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * {@link java.util.concurrent.CountDownLatch} with an associated name for debugging
 */
public class NamedCountDownLatch extends CountDownLatch {

  private String name;

  /**
   * Constructs a {@code CountDownLatch} initialized with the given count.
   *
   * @param count the number of times {@link #countDown} must be invoked before threads can pass through {@link #await}
   * @throws IllegalArgumentException if {@code count} is negative
   */
  public NamedCountDownLatch(String name, int count) {
    super(count);
    this.name = name;
  }

  public String name() {
    return name;
  }

  /**
   * Waits until latch countdown goes to zero. If timeout expires before latch count has gone to zero,
   * then a {@link TimeoutException} will be thrown.
   */
  public void awaitOrThrowWithTimeout(long timeout, TimeUnit timeUnit)
      throws InterruptedException, TimeoutException {
    this.await(timeout, timeUnit);
    if (this.getCount() != 0) {
      throw new TimeoutException("Time expired before latch, " + this.name() + "count went to zero.");
    }
  }

}
