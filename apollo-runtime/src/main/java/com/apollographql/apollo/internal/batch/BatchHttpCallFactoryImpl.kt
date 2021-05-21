package com.apollographql.apollo.internal.batch

import com.apollographql.apollo.api.ScalarTypeAdapters
import okhttp3.Call
import okhttp3.HttpUrl

/**
 * Factory to create [BatchHttpCallImpl] instances
 */
class BatchHttpCallFactoryImpl(
    private val serverUrl: HttpUrl,
    private val httpCallFactory: Call.Factory,
    private val scalarTypeAdapters: ScalarTypeAdapters
) : BatchHttpCallFactory {

  override fun createBatchHttpCall(batch: List<QueryToBatch>): BatchHttpCall {
    return BatchHttpCallImpl(
        batch,
        serverUrl,
        httpCallFactory,
        scalarTypeAdapters
    )
  }
}