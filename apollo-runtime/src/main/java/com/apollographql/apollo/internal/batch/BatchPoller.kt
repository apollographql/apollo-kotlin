package com.apollographql.apollo.internal.batch

import com.apollographql.apollo.api.internal.ApolloLogger
import com.apollographql.apollo.exception.ApolloException
import java.util.LinkedList
import java.util.concurrent.Executor
import java.util.concurrent.TimeUnit
import kotlin.collections.ArrayList

/**
 * Class responsible for holding the queue of requests, and execute them as a batch every X ms,
 * as defined by the BatchConfig.
 */
class BatchPoller(
    private val batchConfig: BatchConfig,
    private val dispatcher: Executor,
    private val batchHttpCallFactory: BatchHttpCallFactory,
    private val logger: ApolloLogger,
    private val periodicJobScheduler: PeriodicJobScheduler = PeriodicJobSchedulerImpl()
) {

  private val queryQueue: LinkedList<QueryToBatch> = LinkedList()

  fun start() {
    stop()
    periodicJobScheduler.schedulePeriodicJob(
        initialDelay = 0,
        interval = batchConfig.batchIntervalMs,
        unit = TimeUnit.MILLISECONDS
    ) {
      synchronized(this) {
        maybeExecuteBatchQuery()
      }
    }
  }

  fun stop() {
    periodicJobScheduler.cancel()
  }

  fun enqueue(query: QueryToBatch) {
    if (!periodicJobScheduler.isRunning()) {
      throw ApolloException("Trying to batch queries without calling ApolloClient.startBatchPoller() first")
    }
    synchronized(this) {
      queryQueue.add(query)
      logger.d("Enqueued Query: ${query.request.operation.name().name()} for batching")
      if (queryQueue.size >= batchConfig.maxBatchSize) {
        // When the amount of queries in the queue reaches the max batch size, trigger the HTTP request without waiting for the batch interval
        maybeExecuteBatchQuery()
      }
    }
  }

  fun removeFromQueue(query: QueryToBatch) {
    synchronized(this) {
      queryQueue.remove(query)
    }
  }

  private fun maybeExecuteBatchQuery() {
    if (queryQueue.isEmpty()) return
    // copy and clear the current queue
    val queryList = ArrayList(queryQueue)
    queryQueue.clear()
    // split into batches
    val batches = queryList.chunked(batchConfig.maxBatchSize)
    logger.d("Executing ${queryList.size} Queries in ${batches.size} Batch(es)")
    for (batch in batches) {
      dispatcher.execute { batchHttpCallFactory.createBatchHttpCall(batch).execute() }
    }
  }
}
