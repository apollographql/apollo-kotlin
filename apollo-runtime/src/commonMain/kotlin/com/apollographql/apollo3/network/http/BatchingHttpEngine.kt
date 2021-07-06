package com.apollographql.apollo3.network.http

import com.apollographql.apollo3.api.AnyAdapter
import com.apollographql.apollo3.api.ApolloRequest
import com.apollographql.apollo3.api.Query
import com.apollographql.apollo3.api.http.DefaultHttpRequestComposerParams
import com.apollographql.apollo3.api.http.HttpBody
import com.apollographql.apollo3.api.http.HttpMethod
import com.apollographql.apollo3.api.http.HttpRequest
import com.apollographql.apollo3.api.http.HttpRequestComposerParams
import com.apollographql.apollo3.api.http.HttpResponse
import com.apollographql.apollo3.api.internal.json.BufferedSinkJsonWriter
import com.apollographql.apollo3.api.internal.json.BufferedSourceJsonReader
import com.apollographql.apollo3.api.internal.json.buildJsonByteString
import com.apollographql.apollo3.api.internal.json.writeArray
import com.apollographql.apollo3.exception.ApolloException
import com.apollographql.apollo3.exception.ApolloHttpException
import com.apollographql.apollo3.internal.BackgroundDispatcher
import com.apollographql.apollo3.internal.DefaultMutex
import com.apollographql.apollo3.mpp.ensureNeverFrozen
import com.apollographql.apollo3.mpp.freeze
import com.apollographql.apollo3.network.http.BatchingHttpEngine.Companion.CAN_BE_BATCHED
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import okio.Buffer
import okio.BufferedSink

/**
 * An [HttpEngine] that wraps another one and batches HTTP queries to execute mutiple
 * at once. This reduces the number of HTTP roundtrips at the price of increased latency as
 * every request in the batch is now as slow as the slowest one.
 * Some servers might have a per-HTTP-call cache making it faster to resolve 1 big array
 * of n queries compared to resolving the n queries separately.
 *
 * Because [ApolloClient.query] suspends, it only makes sense to use query batching when queries are
 * executed from different coroutines. Use [kotlinx.coroutines.async] to create a new coroutine if needed
 *
 * [BatchingHttpEngine] buffers the whole response so it might additionally introduce some
 * client-side latency as it cannot amortize parsing/building the models during network I/O.
 *
 * [BatchingHttpEngine] only works with Post requests. Trying to batch a Get requests is undefined.
 *
 * @param batchIntervalMillis the interval between two batches
 * @param maxBatchSize always send the batch when this threshold is reached
 * @param batchByDefault whether batching is opt-in or opt-out at the request level. See also [canBeBatched]
 */
class BatchingHttpEngine(
    val delegate: HttpEngine = DefaultHttpEngine(),
    val batchIntervalMillis: Long = 10,
    val maxBatchSize: Int = 10,
    val batchByDefault: Boolean = true,
) : HttpEngine {
  private val dispatcher = BackgroundDispatcher()
  private val scope = CoroutineScope(dispatcher.coroutineDispatcher)
  private val mutex = DefaultMutex()

  private val job: Job

  init {
    ensureNeverFrozen(this)
    job = scope.launch {
      while (true) {
        delay(batchIntervalMillis)
        executePendingRequests()
      }
    }
  }

  class PendingRequest(
      val request: HttpRequest,
  ) {
    val deferred = CompletableDeferred<HttpResponse>()
  }

  private val pendingRequests = mutableListOf<PendingRequest>()

  override suspend fun execute(request: HttpRequest): HttpResponse {
    val canBeBatched = request.headers[CAN_BE_BATCHED]?.toBoolean() ?: batchByDefault

    if (!canBeBatched) {
      // Remove the CAN_BE_BATCHED header and forward directly
      return delegate.execute(request.copy(headers = request.headers.filter { it.key != CAN_BE_BATCHED }))
    }

    val pendingRequest = PendingRequest(request)

    val sendNow = mutex.lock {
      // if there was an error, the previous job was already canceled, ignore that error
      pendingRequests.add(pendingRequest)
      pendingRequests.size >= maxBatchSize
    }
    if (sendNow) {
      executePendingRequests()
    }

    return pendingRequest.deferred.await()
  }

  private suspend fun executePendingRequests() {
    val pending = mutex.lock {
      val copy = pendingRequests.toList()
      pendingRequests.clear()
      copy
    }

    if (pending.isEmpty()) {
      return
    }

    val firstRequest = pending.first().request

    val allLengths = pending.map { it.request.headers.get("Content-Length")?.toLongOrNull() ?: -1L }
    val contentLength = if (allLengths.contains(-1)) {
      -1
    } else {
      allLengths.sum()
    }

    val allBodies = pending.mapNotNull { it.request.body }

    val body = object : HttpBody {
      override val contentType = "application/json"
      override val contentLength = contentLength
      override fun writeTo(bufferedSink: BufferedSink) {
        val writer = BufferedSinkJsonWriter(bufferedSink)
        writer.writeArray {
          this as BufferedSinkJsonWriter
          allBodies.forEach { body ->
            val buffer = Buffer()
            body.writeTo(buffer)
            jsonValue(buffer.readUtf8())
          }
        }
      }
    }

    val request = HttpRequest(
        method = HttpMethod.Post,
        url = firstRequest.url,
        headers = emptyMap(),
        body = body,
    )
    freeze(request)

    val result = kotlin.runCatching {
      val response = delegate.execute(request)
      if (response.statusCode !in 200..299) {
        throw ApolloHttpException(response.statusCode, response.headers, "HTTP error ${response.statusCode} while executing batched query: '${response.body?.readUtf8()}'")
      }
      val responseBody = response.body ?: throw ApolloException("null body when executing batched query")

      // TODO: this is most likely going to transform BitNumbers into strings, not sure how much of an issue that is
      val list = AnyAdapter.fromJson(BufferedSourceJsonReader(responseBody))
      if (list !is List<*>) throw ApolloException("batched query response is not a list when executing batched query")

      if (list.size != pending.size) {
        throw ApolloException("batched query response count (${list.size}) does not match the requested queries (${pending.size})")
      }

      list.map {
        if (it == null) {
          throw ApolloException("batched query response contains a null item")
        }
        buildJsonByteString {
          AnyAdapter.toJson(this, it)
        }
      }
    }

    val failure = result.exceptionOrNull()
    if (failure != null) {
      pending.forEach {
        it.deferred.completeExceptionally(failure)
      }
      return
    }

    result.getOrThrow().forEachIndexed { index, byteString ->
      // This works because the server must return the responses in order
      pending[index].deferred.complete(
          HttpResponse(
              statusCode = 200,
              headers = emptyMap(),
              bodyString = byteString,
              bodySource = null
          )
      )
    }
  }

  override fun dispose() {
    scope.cancel()
    dispatcher.dispose()
  }

  companion object {
    const val CAN_BE_BATCHED = "X-APOLLO-CAN-BE-BATCHED"
  }
}

fun <D : Query.Data> ApolloRequest<D>.canBeBatched(canBeBatched: Boolean): ApolloRequest<D> {
  val context = executionContext[HttpRequestComposerParams] ?: DefaultHttpRequestComposerParams

  return withExecutionContext(context.copy(headers = context.headers + (CAN_BE_BATCHED to canBeBatched.toString())))
}