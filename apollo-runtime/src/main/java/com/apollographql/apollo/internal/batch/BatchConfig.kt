package com.apollographql.apollo.internal.batch

import java.util.concurrent.TimeUnit

/**
 * Configuration parameters for batching.
 * @property batchingEnabled whether to enable or disable batching for the whole [com.apollographql.apollo.ApolloClient]. Default is false.
 * @property batchIntervalMs The interval in milliseconds to check for queries to be sent as a batched HTTP call. Must be greater than 0. Default is 10ms.
 * @property maxBatchSize The maximum number of queries to be sent in a single batched HTTP call. Default is 10.
 * @see com.apollographql.apollo.ApolloClient.Builder.batchingConfiguration
 */
data class BatchConfig(
    val batchingEnabled: Boolean = false,
    val batchIntervalMs: Long = TimeUnit.MILLISECONDS.toMillis(10),
    val maxBatchSize: Int = 10
)
