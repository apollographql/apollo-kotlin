package com.apollographql.apollo.internal.batch

import com.apollographql.apollo.api.ScalarTypeAdapters
import com.apollographql.apollo.api.cache.http.HttpCache
import com.apollographql.apollo.api.cache.http.HttpCachePolicy
import com.apollographql.apollo.api.internal.Optional
import com.apollographql.apollo.api.internal.json.BufferedSourceJsonReader
import com.apollographql.apollo.api.internal.json.JsonWriter
import com.apollographql.apollo.api.internal.json.ResponseJsonStreamReader
import com.apollographql.apollo.api.internal.json.Utils
import com.apollographql.apollo.api.internal.json.writeArray
import com.apollographql.apollo.cache.ApolloCacheHeaders
import com.apollographql.apollo.exception.ApolloException
import com.apollographql.apollo.interceptor.ApolloInterceptor
import com.apollographql.apollo.internal.interceptor.ApolloServerInterceptor
import okhttp3.Call
import okhttp3.Callback
import okhttp3.HttpUrl
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response
import okhttp3.ResponseBody
import okio.Buffer
import okio.ByteString
import java.io.IOException
import java.nio.charset.Charset

/**
 * Represents a single HTTP call that requests data for a list of queries at once
 */
class BatchHttpCallImpl(
    private val queryList: List<QueryToBatch>,
    private val serverUrl: HttpUrl,
    private val httpCallFactory: Call.Factory,
    private val scalarTypeAdapters: ScalarTypeAdapters
) : BatchHttpCall {

  override fun execute() {
    val queryRequestBodyList = mutableListOf<ByteString>()

    for (query in queryList) {
      // Trigger fetch started callback
      query.callback.onFetch(ApolloInterceptor.FetchSourceType.NETWORK)
      // Compose request body for each query
      queryRequestBodyList.add(
          query.request.operation.composeRequestBody(
              query.request.autoPersistQueries,
              query.request.sendQueryDocument,
              scalarTypeAdapters
          )
      )
    }
    // Combine all request bodies into one
    val batchRequestBody = RequestBody.create(
        ApolloServerInterceptor.MEDIA_TYPE,
        createBatchRequestJsonBody(queryRequestBodyList)
    )
    // Batching is only supported for POST Calls, and doesn't support HTTP cache
    val requestBuilder = Request.Builder()
        .url(serverUrl)
        .header(ApolloServerInterceptor.HEADER_ACCEPT_TYPE, ApolloServerInterceptor.ACCEPT_TYPE)
        .header(ApolloServerInterceptor.HEADER_CONTENT_TYPE, ApolloServerInterceptor.CONTENT_TYPE)
        .post(batchRequestBody)
    // Assumes all queries in the batch have the same headers
    val firstRequest = queryList.asSequence().map { it.request }.first()
    for (header in firstRequest.requestHeaders.headers()) {
      val value = firstRequest.requestHeaders.headerValue(header)
      requestBuilder.header(header, value)
    }

    // execute the batch http call
    val httpCall = httpCallFactory.newCall(requestBuilder.build())
    httpCall.enqueue(object : Callback {
      override fun onResponse(call: Call, response: Response) {
        try {
          // Extract individual responses from the batch response body
          val responseBodies = extractResponseListFromBody(response)

          // Assert that we got as many responses than we asked for
          if (responseBodies.size != queryList.size) {
            throw ApolloException("Batch response has missing data, expected ${queryList.size}, got ${responseBodies.size}")
          }

          // Callback individual interceptor chains with the corresponding extracted responses
          queryList.forEachIndexed { index, queryToBatch ->
            responseBodies[index].let { response ->
              queryToBatch.callback.onResponse(ApolloInterceptor.InterceptorResponse(response))
              queryToBatch.callback.onCompleted()
            }
          }
        } catch (exception: Exception) {
          queryList.forEach {
            val message = "Failed to parse batch http response for operation '${it.request.operation.name().name()}'"
            it.callback.onFailure(ApolloException(message, exception))
          }
        } finally {
          response.close()
        }
      }

      override fun onFailure(call: Call, e: IOException) {
        queryList.forEach {
          val message = "Failed to execute http call for operation '${it.request.operation.name().name()}'"
          it.callback.onFailure(ApolloException(message, e))
        }
      }
    })
  }

  private fun createBatchRequestJsonBody(queryRequestBodyList: List<ByteString>): ByteString {
    val buffer = Buffer()
    JsonWriter.of(buffer).use { writer ->
      writer.writeArray {
        queryRequestBodyList.forEach {
          jsonValue(it.string(Charset.defaultCharset()))
        }
      }
    }
    return buffer.readByteString()
  }

  private fun extractResponseListFromBody(response: Response): List<Response> {
    return response.body()?.source()?.let { body ->
      val reader = ResponseJsonStreamReader(BufferedSourceJsonReader(body))
      val responseList = reader.readList()?.map { dataMap ->
        val buffer = Buffer()
        JsonWriter.of(buffer).use { writer -> Utils.writeToJson(dataMap, writer) }
        buffer.readByteString()
      } ?: throw ApolloException("Unable to extract individual responses from batch response body")
      responseList.map { responseBodyString ->
        response.newBuilder()
            .body(ResponseBody.create(ApolloServerInterceptor.MEDIA_TYPE, responseBodyString))
            .build()
      }
    } ?: throw ApolloException("Unable to read batch response body")
  }
}