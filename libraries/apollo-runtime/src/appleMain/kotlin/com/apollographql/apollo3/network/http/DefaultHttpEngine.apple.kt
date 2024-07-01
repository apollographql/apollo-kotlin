package com.apollographql.apollo.network.http

import com.apollographql.apollo.api.http.HttpHeader
import com.apollographql.apollo.api.http.HttpMethod
import com.apollographql.apollo.api.http.HttpRequest
import com.apollographql.apollo.api.http.HttpResponse
import com.apollographql.apollo.exception.ApolloNetworkException
import com.apollographql.apollo.network.toNSData
import kotlinx.cinterop.alloc
import kotlinx.cinterop.nativeHeap
import kotlinx.cinterop.ptr
import kotlinx.coroutines.suspendCancellableCoroutine
import okio.Buffer
import okio.BufferedSource
import okio.ByteString.Companion.toByteString
import okio.Sink
import okio.Source
import okio.Timeout
import okio.buffer
import platform.Foundation.NSCachedURLResponse
import platform.Foundation.NSData
import platform.Foundation.NSError
import platform.Foundation.NSHTTPURLResponse
import platform.Foundation.NSMutableURLRequest
import platform.Foundation.NSURL
import platform.Foundation.NSURLRequestReloadIgnoringCacheData
import platform.Foundation.NSURLResponse
import platform.Foundation.NSURLSession
import platform.Foundation.NSURLSessionConfiguration
import platform.Foundation.NSURLSessionDataDelegateProtocol
import platform.Foundation.NSURLSessionDataTask
import platform.Foundation.NSURLSessionResponseAllow
import platform.Foundation.NSURLSessionResponseDisposition
import platform.Foundation.NSURLSessionTask
import platform.Foundation.setHTTPBody
import platform.Foundation.setHTTPMethod
import platform.Foundation.setValue
import platform.darwin.NSObject
import platform.posix.pthread_cond_broadcast
import platform.posix.pthread_cond_destroy
import platform.posix.pthread_cond_init
import platform.posix.pthread_cond_t
import platform.posix.pthread_cond_wait
import platform.posix.pthread_mutex_destroy
import platform.posix.pthread_mutex_init
import platform.posix.pthread_mutex_lock
import platform.posix.pthread_mutex_t
import platform.posix.pthread_mutex_unlock

actual fun DefaultHttpEngine(timeoutMillis: Long): HttpEngine = AppleHttpEngine(timeoutMillis)

fun DefaultHttpEngine(
    timeoutMillis: Long = 60_000,
    nsUrlSessionConfiguration: NSURLSessionConfiguration,
): HttpEngine = AppleHttpEngine(timeoutMillis, nsUrlSessionConfiguration)

private class AppleHttpEngine(
    private val timeoutMillis: Long,
    private val nsUrlSessionConfiguration: NSURLSessionConfiguration,
) : HttpEngine {

  constructor(timeoutMillis: Long) : this(
      timeoutMillis = timeoutMillis,
      nsUrlSessionConfiguration = NSURLSessionConfiguration.defaultSessionConfiguration()
  )

  private val delegate = StreamingDataDelegate()

  private val nsUrlSession = NSURLSession.sessionWithConfiguration(
      configuration = nsUrlSessionConfiguration,
      delegate = delegate,
      delegateQueue = null
  )

  override suspend fun execute(request: HttpRequest): HttpResponse = suspendCancellableCoroutine { continuation ->
    val nsMutableURLRequest = NSMutableURLRequest.requestWithURL(
        URL = NSURL(string = request.url)
    ).apply {
      setTimeoutInterval(timeoutMillis.toDouble() / 1000)

      request.headers.forEach {
        setValue(it.value, forHTTPHeaderField = it.name)
      }

      if (request.method == HttpMethod.Get) {
        setHTTPMethod("GET")
      } else {
        setHTTPMethod("POST")
        val requestBody = request.body
        if (requestBody != null) {
          setValue(requestBody.contentType, forHTTPHeaderField = "Content-Type")

          if (requestBody.contentLength >= 0) {
            setValue(requestBody.contentLength.toString(), forHTTPHeaderField = "Content-Length")
          }
          val body = Buffer().apply { requestBody.writeTo(this) }.readByteArray().toNSData()
          setHTTPBody(body)
        }
      }
      setCachePolicy(NSURLRequestReloadIgnoringCacheData)
    }

    val httpDataPipe = Pipe()
    val httpDataSink = httpDataPipe.sink.buffer()
    val httpDataSource = httpDataPipe.source.buffer()

    val handler = object : StreamingDataDelegate.Handler {
      private var hasDispatchedHttpResponse = false

      override fun onResponse(response: NSHTTPURLResponse) {
        continuation.resumeWith(
            buildHttpResponse(
                data = httpDataSource,
                httpResponse = response,
                error = null,
            )
        )
        hasDispatchedHttpResponse = true
      }

      override fun onData(data: NSData) {
        httpDataSink.write(data.toByteString())
        // Typically here we'll receive a chunk for each incremental payload. Flushing here makes them available to the reader
        // as soon as they're available.
        httpDataSink.flush()
      }

      override fun onComplete(error: NSError?) {
        httpDataSink.close()
        // This can be called with an error before `onResponse` if there's an error like no connectivity.
        // It if it called *after* `onResponse`, do not resume the continuation again.
        if (error != null && !hasDispatchedHttpResponse) continuation.resumeWith(
            buildHttpResponse(
                data = httpDataSource,
                httpResponse = null,
                error = error,
            )
        )
      }
    }

    val task = nsUrlSession.dataTaskWithRequest(nsMutableURLRequest)
    delegate.registerHandlerForTask(task, handler)

    continuation.invokeOnCancellation {
      task.cancel()
    }
    task.resume()
  }

  override fun close() {
    nsUrlSession.invalidateAndCancel()
  }
}

private fun buildHttpResponse(
    data: BufferedSource,
    httpResponse: NSHTTPURLResponse?,
    error: NSError?,
): Result<HttpResponse> {

  if (error != null) {
    return Result.failure(
        ApolloNetworkException(
            message = "Failed to execute GraphQL http network request",
            platformCause = error
        )
    )
  }

  if (httpResponse == null) {
    return Result.failure(
        ApolloNetworkException("Failed to parse GraphQL http network response: EOF")
    )
  }

  val httpHeaders = httpResponse.allHeaderFields
      .map { (key, value) ->
        HttpHeader(key.toString(), value.toString())
      }

  val statusCode = httpResponse.statusCode.toInt()

  return Result.success(
      HttpResponse.Builder(statusCode = statusCode)
          .headers(httpHeaders)
          .body(bodySource = data)
          .build()
  )
}

/**
 * Order of the callbacks in the no-error case:
 * - didReceiveResponse
 * - didReceiveData
 * - didReceiveData
 * ...
 * - willCacheResponse
 *
 * Error case (e.g. no connectivity):
 * - didCompleteWithError (with a non-null error)
 */
private class StreamingDataDelegate : NSObject(), NSURLSessionDataDelegateProtocol {
  interface Handler {
    fun onResponse(response: NSHTTPURLResponse)
    fun onData(data: NSData)
    fun onComplete(error: NSError?)
  }

  private val handlers = mutableMapOf<NSURLSessionTask, Handler>()

  override fun URLSession(
      session: NSURLSession,
      dataTask: NSURLSessionDataTask,
      didReceiveResponse: NSURLResponse,
      completionHandler: (NSURLSessionResponseDisposition) -> Unit,
  ) {
    handlers[dataTask]?.onResponse(didReceiveResponse as NSHTTPURLResponse)
    completionHandler(NSURLSessionResponseAllow)
  }

  override fun URLSession(
      session: NSURLSession,
      dataTask: NSURLSessionDataTask,
      didReceiveData: NSData,
  ) {
    handlers[dataTask]?.onData(didReceiveData)
  }

  override fun URLSession(
      session: NSURLSession,
      task: NSURLSessionTask,
      didCompleteWithError: NSError?,
  ) {
    handlers[task]?.onComplete(didCompleteWithError)

    // Cleanup
    handlers.remove(task)
  }

  override fun URLSession(
      session: NSURLSession,
      dataTask: NSURLSessionDataTask,
      willCacheResponse: NSCachedURLResponse,
      completionHandler: (NSCachedURLResponse?) -> Unit,
  ) {
    handlers[dataTask]?.onComplete(null)

    // Cleanup
    handlers.remove(dataTask)

    completionHandler(willCacheResponse)
  }

  fun registerHandlerForTask(task: NSURLSessionTask, handler: Handler) {
    handlers[task] = handler
  }
}

/**
 * A source and a sink that are attached. The sink's output is the source's input.
 * A [Buffer] is used internally to store the data.
 * A `pthread` `mutex` and `cond` are used to block the source's reading until there is data to read, or the sink is closed. These are
 * released when both the sink and the source are closed.
 *
 * Inspired by okio's Pipe which is JVM only.
 */
private class Pipe {
  private val buffer = Buffer()

  private val mutex = nativeHeap.alloc<pthread_mutex_t>()
  private val cond = nativeHeap.alloc<pthread_cond_t>()

  private var isSinkClosed = false
  private var isSourceClosed = false

  init {
    pthread_mutex_init(mutex.ptr, null)
    pthread_cond_init(cond.ptr, null)
  }

  val sink: Sink = object : Sink {
    override fun write(source: Buffer, byteCount: Long) {
      pthread_mutex_lock(mutex.ptr)
      buffer.write(source, byteCount)
      pthread_cond_broadcast(cond.ptr)
      pthread_mutex_unlock(mutex.ptr)
    }

    override fun close() {
      pthread_mutex_lock(mutex.ptr)
      isSinkClosed = true
      pthread_cond_broadcast(cond.ptr)
      pthread_mutex_unlock(mutex.ptr)

      disposeIfClosed()
    }

    override fun flush() {}
    override fun timeout() = Timeout.NONE
  }

  val source: Source = object : Source {
    override fun read(sink: Buffer, byteCount: Long): Long {
      pthread_mutex_lock(mutex.ptr)
      while (buffer.size == 0L && !isSinkClosed) {
        pthread_cond_wait(cond.ptr, mutex.ptr)
      }
      val readCount = buffer.read(sink, byteCount)
      pthread_mutex_unlock(mutex.ptr)
      return readCount
    }

    override fun close() {
      isSourceClosed = true
      disposeIfClosed()
    }

    override fun timeout() = Timeout.NONE
  }

  private fun disposeIfClosed() {
    if (isSourceClosed && isSinkClosed) {
      pthread_mutex_destroy(mutex.ptr)
      pthread_cond_destroy(cond.ptr)
    }
  }
}
