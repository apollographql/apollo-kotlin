package com.apollographql.apollo3.network.http

import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.api.AnyAdapter
import com.apollographql.apollo3.api.ApolloRequest
import com.apollographql.apollo3.api.HasMutableExecutionContext
import com.apollographql.apollo3.api.Operation
import com.apollographql.apollo3.api.http.HttpBody
import com.apollographql.apollo3.api.http.HttpMethod
import com.apollographql.apollo3.api.http.HttpRequest
import com.apollographql.apollo3.api.http.HttpResponse
import com.apollographql.apollo3.api.http.httpHeader
import com.apollographql.apollo3.api.http.valueOf
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
 * An [HttpEngine] that wraps another one and batches HTTP queries to execute multiple
 * at once. This reduces the number of HTTP round trips at the price of increased latency as
 * every request in the batch is now as slow as the slowest one.
 * Some servers might have a per-HTTP-call cache making it faster to resolve 1 big array
 * of n queries compared to resolving the n queries separately.
 *
 * Because [ApolloCall.execute] suspends, it only makes sense to use query batching when queries are
 * executed from different coroutines. Use [async] to create a new coroutine if needed
 *
 * [BatchingHttpEngine] buffers the whole response so it might additionally introduce some
 * client-side latency as it cannot amortize parsing/building the models during network I/O.
 *
 * [BatchingHttpEngine] only works with Post requests. Trying to batch a Get requests is undefined.
 *
 * @param batchIntervalMillis the interval between two batches
 * @param maxBatchSize always send the batch when this threshold is reached
 */
class BatchingHttpEngine(
    val delegate: HttpEngine = HttpEngine(),
    val batchIntervalMillis: Long = 10,
    val maxBatchSize: Int = 10,
) : HttpEngine {
  private val dispatcher = BackgroundDispatcher()
  private val scope = CoroutineScope(dispatcher.coroutineDispatcher)
  private val mutex = DefaultMutex()
  private var disposed = false

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
    // Batching is enabled by default, unless explicitly disabled
    val canBeBatched = request.headers.valueOf(CAN_BE_BATCHED)?.toBoolean() ?: true

    if (!canBeBatched) {
      // Remove the CAN_BE_BATCHED header and forward directly
      return delegate.execute(request.newBuilder().addHeaders(headers = request.headers.filter { it.name != CAN_BE_BATCHED }).build())
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

    val allLengths = pending.map { it.request.headers.valueOf("Content-Length")?.toLongOrNull() ?: -1L }
    val contentLength = if (allLengths.contains(-1)) {
      -1
    } else {
      allLengths.sum()
    }

    val allBodies = pending.map { it.request.body ?: error("empty body while batching queries") }

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

    val request = HttpRequest.Builder(
        method = HttpMethod.Post,
        url = firstRequest.url,
    ).body(
        body = body,
    ).build()

    freeze(request)

    var exception: ApolloException? = null
    val result = try {
      val response = delegate.execute(request)
      if (response.statusCode !in 200..299) {
        throw ApolloHttpException(response.statusCode, response.headers, "HTTP error ${response.statusCode} while executing batched query: '${response.body?.readUtf8()}'")
      }
      val responseBody = response.body ?: throw ApolloException("null body when executing batched query")

      // TODO: this is most likely going to transform BigNumbers into strings, not sure how much of an issue that is
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
    } catch (e: ApolloException) {
      exception = e
      null
    }

    if (exception != null) {
      pending.forEach {
        it.deferred.completeExceptionally(exception)
      }
      return
    } else {
      result!!.forEachIndexed { index, byteString ->
        // This works because the server must return the responses in order
        pending[index].deferred.complete(
            HttpResponse.Builder(statusCode = 200)
                .body(byteString)
                .build()
        )
      }
    }
  }

  override fun dispose() {
    if (!disposed) {
      delegate.dispose()
      scope.cancel()
      dispatcher.dispose()
      disposed = true
    }
  }

  companion object {
    const val CAN_BE_BATCHED = "X-APOLLO-CAN-BE-BATCHED"
  }
}

fun <T> HasMutableExecutionContext<T>.canBeBatched(canBeBatched: Boolean) where T : HasMutableExecutionContext<T> = httpHeader(
    CAN_BE_BATCHED, canBeBatched.toString()
)

@Deprecated("Please use ApolloClient.Builder methods instead. This will be removed in v3.0.0.")
fun ApolloClient.withCanBeBatched(canBeBatched: Boolean) = newBuilder().canBeBatched(canBeBatched).build()

@Deprecated("Please use ApolloRequest.Builder methods instead. This will be removed in v3.0.0.")
fun <D : Operation.Data> ApolloRequest<D>.withCanBeBatched(canBeBatched: Boolean) = newBuilder().canBeBatched(canBeBatched).build()
