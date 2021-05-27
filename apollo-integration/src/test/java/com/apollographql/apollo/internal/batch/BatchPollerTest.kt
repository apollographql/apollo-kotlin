package com.apollographql.apollo.internal.batch

import com.apollographql.apollo.Utils
import com.apollographql.apollo.api.Input
import com.apollographql.apollo.api.internal.ApolloLogger
import com.apollographql.apollo.exception.ApolloException
import com.apollographql.apollo.integration.interceptor.AllFilmsQuery
import com.apollographql.apollo.interceptor.ApolloInterceptor
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import java.util.concurrent.TimeUnit

class BatchPollerTest {
  private lateinit var batchPoller: BatchPoller

  private val query = AllFilmsQuery.builder()
      .after("some cursor")
      .beforeInput(Input.absent())
      .firstInput(Input.fromNullable(null))
      .last(100)
      .build()
  private val callback = mock(ApolloInterceptor.CallBack::class.java)
  private val queryToBatch = QueryToBatch(ApolloInterceptor.InterceptorRequest.builder(query).build(), callback)

  private val dispatcher = Utils.immediateExecutorService()
  private val factory = TestBatchHttpCallFactory()
  private val periodicJobScheduler = TestPeriodicJobScheduler()

  @Before
  fun setup() {
    batchPoller = BatchPoller(
        batchConfig = BatchConfig(batchingEnabled = true, batchIntervalMs = 10, maxBatchSize = 4),
        dispatcher = dispatcher,
        batchHttpCallFactory = factory,
        logger = ApolloLogger(null),
        periodicJobScheduler = periodicJobScheduler
    )
  }

  @Test(expected = ApolloException::class)
  fun testEnqueueWithoutStart() {
    batchPoller.enqueue(queryToBatch)
  }

  @Test
  fun testEnqueue() {
    batchPoller.start()
    assert(periodicJobScheduler.isRunning())

    batchPoller.enqueue(queryToBatch)
    assert(factory.batchesProcessed.size == 0)
    periodicJobScheduler.triggerPeriodicJob()
    assert(factory.batchesProcessed.size == 1)

    batchPoller.enqueue(queryToBatch)
    batchPoller.enqueue(queryToBatch)
    batchPoller.enqueue(queryToBatch)
    periodicJobScheduler.triggerPeriodicJob()
    assert(factory.batchesProcessed.size == 2)

    batchPoller.enqueue(queryToBatch)
    batchPoller.enqueue(queryToBatch)
    periodicJobScheduler.triggerPeriodicJob()
    assert(factory.batchesProcessed.size == 3)

    assert(factory.batchesProcessed[0].executedQueries.size == 1)
    assert(factory.batchesProcessed[1].executedQueries.size == 3)
    assert(factory.batchesProcessed[2].executedQueries.size == 2)

    batchPoller.stop()
    assert(!periodicJobScheduler.isRunning())
  }

  @Test
  fun testRemoveQueryBeforeSend() {
    batchPoller.start()
    batchPoller.enqueue(queryToBatch)
    batchPoller.removeFromQueue(queryToBatch)
    periodicJobScheduler.triggerPeriodicJob()
    assert(factory.batchesProcessed.size == 0)
  }

  @Test
  fun testMaxBatchSizeReached() {
    batchPoller.start()
    batchPoller.enqueue(queryToBatch)
    batchPoller.enqueue(queryToBatch)
    batchPoller.enqueue(queryToBatch)
    batchPoller.enqueue(queryToBatch)
    assert(factory.batchesProcessed.size == 1)
    assert(factory.batchesProcessed[0].executedQueries.size == 4)
  }

  @Test
  fun testMaxSizeAndPeriodicTrigger() {
    batchPoller.start()
    batchPoller.enqueue(queryToBatch)
    batchPoller.enqueue(queryToBatch)
    batchPoller.enqueue(queryToBatch)
    batchPoller.enqueue(queryToBatch)
    batchPoller.enqueue(queryToBatch)
    periodicJobScheduler.triggerPeriodicJob()

    assert(factory.batchesProcessed.size == 2)
    assert(factory.batchesProcessed[0].executedQueries.size == 4)
    assert(factory.batchesProcessed[1].executedQueries.size == 1)
  }

  @Test(expected = ApolloException::class)
  fun testEnqueueAfterStop() {
    batchPoller.start()
    batchPoller.enqueue(queryToBatch)
    batchPoller.stop()
    batchPoller.enqueue(queryToBatch)
  }

  private class TestPeriodicJobScheduler(
      private val testExecutor: Utils.TestExecutor = Utils.TestExecutor()
  ) : PeriodicJobScheduler {
    var started = false

    fun triggerPeriodicJob() {
      testExecutor.triggerActions()
    }

    override fun schedulePeriodicJob(initialDelay: Long, interval: Long, unit: TimeUnit, job: () -> Unit) {
      started = true
      testExecutor.execute { job() }
    }

    override fun cancel() {
      started = false
    }

    override fun isRunning(): Boolean {
      return started
    }
  }

  private class TestBatchHttpCallFactory : BatchHttpCallFactory {
    var batchesProcessed = mutableListOf<BatchHttpCallTest>()
    override fun createBatchHttpCall(batch: List<QueryToBatch>): BatchHttpCall {
      return BatchHttpCallTest(batch).also { batchesProcessed.add(it) }
    }
  }

  class BatchHttpCallTest(private val batch: List<QueryToBatch>) : BatchHttpCall {
    lateinit var executedQueries: List<QueryToBatch>
    override fun execute() {
      executedQueries = batch
    }
  }
}