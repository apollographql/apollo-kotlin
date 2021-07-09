package com.apollographql.apollo3.cache.normalized.internal

import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.toList
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.fail

/**
 * XXX: move to commonTest
 */
class ReentrantChannelTest {
  @Test
  fun noAdditionalCapacityDeadlocks() {
    val changedKeysEvents = MutableSharedFlow<Set<String>>()

    val changedKeys = changedKeysEvents.asSharedFlow()

    runBlocking {
      launch {
        delay(100)
        changedKeysEvents.emit(setOf("1001"))
      }
      try {
        withTimeout(1000) {
          changedKeys.collect {
            // This emit will deadlock since it's waiting for it's own collector
            changedKeysEvents.emit(setOf("1001"))
            cancel()
          }
        }
        fail("A timeout was expected")
      } catch (e: TimeoutCancellationException) {

      }
    }

  }

  @Test
  fun emittingMoreThanTheCapacityDeadlocks() {
    val capacity = 5
    val changedKeysEvents = MutableSharedFlow<Set<String>>(capacity)

    val changedKeys = changedKeysEvents.asSharedFlow()

    runBlocking {
      launch {
        delay(100)
        changedKeysEvents.emit(setOf("1001"))
      }

      try {
        withTimeout(1000) {
          changedKeys.collect {
            repeat(capacity + 1) {
              changedKeysEvents.emit(setOf("1001"))
            }
            cancel()
          }
        }
        fail("A timeout was expected")
      } catch (e: TimeoutCancellationException) {

      }
    }
  }

  @Test
  fun emittingTheCapacityDoesNotDeadlock() {
    val changedKeysEvents = MutableSharedFlow<Set<String>>(1)

    val changedKeys = changedKeysEvents.asSharedFlow()

    runBlocking {
      launch {
        delay(100)
        // kickoff collection
        changedKeysEvents.emit(setOf("1001"))
      }

      val list = changedKeys.onEach {
        changedKeysEvents.emit(setOf("1001"))
      }.take(5)
          .toList()

      assertEquals(0.until(5).map { setOf("1001") }, list)
    }
  }

  @Test
  fun emittingMoreThanOneItemWorksIfTakingOnlyTheFirstItem() {
    val changedKeysEvents = MutableSharedFlow<Set<String>>(2)

    val changedKeys = changedKeysEvents.asSharedFlow()

    runBlocking {
      launch {
        delay(100)
        // kickoff collection
        changedKeysEvents.emit(setOf("1001"))
      }

      val list = changedKeys.onEach {
        // The second time we come here, there is already one item in the buffer so we'll only be able to emit one
        changedKeysEvents.emit(setOf("1001"))
        changedKeysEvents.emit(setOf("1001"))
      }.take(1)
          .toList()

      assertEquals(0.until(1).map { setOf("1001") }, list)
    }
  }

  @Test
  fun multipleCoroutinesDeadlock() {
    val parallelism = 2
    val itemCount = 5

    val changedKeysEvents = MutableSharedFlow<Set<String>>(parallelism)
    val changedKeys = changedKeysEvents.asSharedFlow()

    val collectedItems = mutableListOf<Set<String>>()

    runBlocking {
      val jobs = 0.until(parallelism).map {
        launch {
          changedKeys.onEach {
            // Because each coroutine will receive the updates from all the other ones
            // This will make the buffer grow past its capacity.
            // With a parallelism of 2, 2 events are emitted for each collection.
            changedKeysEvents.emit(setOf("1001"))

            collectedItems.add(it)
          }.take(itemCount)
              .toList()
        }
      }

      delay(100)
      // kickoff collection
      changedKeysEvents.emit(setOf("1001"))

      try {
        withTimeout(500) {
          jobs.forEach { it.join() }
        }
      } catch (e: TimeoutCancellationException) {
        // make sure the test terminates else it will wait for all jobs
        jobs.forEach { it.cancelAndJoin() }
      }
    }
  }
}