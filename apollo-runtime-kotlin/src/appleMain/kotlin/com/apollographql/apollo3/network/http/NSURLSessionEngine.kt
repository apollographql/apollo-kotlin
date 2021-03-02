package com.apollographql.apollo3.network.http

import com.apollographql.apollo3.exception.ApolloException
import com.apollographql.apollo3.exception.ApolloHttpException
import com.apollographql.apollo3.exception.ApolloNetworkException
import kotlinx.cinterop.COpaquePointer
import kotlinx.cinterop.StableRef
import kotlinx.cinterop.asStableRef
import kotlinx.cinterop.staticCFunction
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.suspendCancellableCoroutine
import okio.Buffer
import okio.IOException
import okio.toByteString
import platform.Foundation.NSData
import platform.Foundation.NSError
import platform.Foundation.NSHTTPURLResponse
import platform.Foundation.NSMutableURLRequest
import platform.Foundation.NSThread
import platform.Foundation.NSURL
import platform.Foundation.NSURLRequest
import platform.Foundation.NSURLRequestReloadIgnoringCacheData
import platform.Foundation.NSURLResponse
import platform.Foundation.NSURLSession
import platform.Foundation.NSURLSessionConfiguration
import platform.Foundation.NSURLSessionDataTask
import platform.Foundation.dataTaskWithRequest
import platform.Foundation.setHTTPMethod
import platform.Foundation.setValue
import platform.darwin.dispatch_async_f
import platform.darwin.dispatch_get_main_queue
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.native.concurrent.freeze

typealias UrlSessionDataTaskCompletionHandler = (NSData?, NSURLResponse?, NSError?) -> Unit

fun interface DataTaskFactory {
  fun dataTask(request: NSURLRequest, completionHandler: UrlSessionDataTaskCompletionHandler): NSURLSessionDataTask
}

@ExperimentalCoroutinesApi
actual class DefaultHttpEngine(
    private val dataTaskFactory: DataTaskFactory,
    private val connectTimeoutMillis: Long = 60_000,
): HttpEngine {

  actual constructor(
      connectTimeoutMillis: Long,
      readTimeoutMillis: Long,
  ) : this(
      dataTaskFactory = DefaultDataTaskFactory(readTimeoutMillis),
      connectTimeoutMillis = connectTimeoutMillis,
  )

  private class DefaultDataTaskFactory(readTimeoutMillis: Long) : DataTaskFactory {
    private val nsurlSession = NSURLSession.sessionWithConfiguration(NSURLSessionConfiguration.defaultSessionConfiguration().apply {
      timeoutIntervalForRequest = readTimeoutMillis.toDouble() / 1000
    })

    override fun dataTask(request: NSURLRequest, completionHandler: UrlSessionDataTaskCompletionHandler): NSURLSessionDataTask {
      return nsurlSession.dataTaskWithRequest(request, completionHandler)
    }
  }

  @Suppress("UNCHECKED_CAST")
  override suspend fun <R> execute(request: HttpRequest, block: (HttpResponse) -> R): R {
    assert(NSThread.isMainThread())

    request.freeze()

    val result = suspendCancellableCoroutine<Result<R>> { continuation ->
      val continuationRef = StableRef.create(continuation).asCPointer()
      val delegate = { httpData: NSData?, nsUrlResponse: NSURLResponse?, error: NSError? ->
        initRuntimeIfNeeded()

        parse(
            data = httpData,
            httpResponse = nsUrlResponse as? NSHTTPURLResponse,
            error = error,
            block = block
        ).dispatchOnMain(continuationRef)
      }

      val nsMutableURLRequest = NSMutableURLRequest.requestWithURL(
          URL = NSURL(string = request.url)
      ).apply {
        setTimeoutInterval(connectTimeoutMillis.toDouble() / 1000)
        if (request.method == HttpMethod.Get) {
          setHTTPMethod("GET")
        } else {
          setHTTPMethod("POST")
        }
        request.headers.forEach {
          setValue(it.value, forHTTPHeaderField = it.key)
        }
        setCachePolicy(NSURLRequestReloadIgnoringCacheData)
      }

      dataTaskFactory.dataTask(nsMutableURLRequest.freeze(), delegate.freeze())
          .also { task ->
            continuation.invokeOnCancellation {
              task.cancel()
            }
          }
          .resume()
    }

    when (result) {
      is Result.Success -> return result.response as R
      is Result.Failure -> throw result.cause
    }
  }


  private fun <R> parse(
      data: NSData?,
      httpResponse: NSHTTPURLResponse?,
      error: NSError?,
      block: (HttpResponse) -> R
  ): Result<R> {
    if (error != null) return Result.Failure(
        cause = ApolloNetworkException(
            message = "Failed to execute GraphQL http network request",
            cause = IOException(error.localizedDescription)
        )
    )

    if (httpResponse == null) return Result.Failure(
        cause = ApolloNetworkException("Failed to parse GraphQL http network response: EOF")
    )

    val httpHeaders = httpResponse.allHeaderFields
        .map { (key, value) -> key.toString() to value.toString() }
        .toMap()

    val statusCode = httpResponse.statusCode.toInt()

    /**
     * XXX: Can data be null?
     */
    if (data == null) return Result.Failure(
        cause = ApolloHttpException(
            statusCode = httpResponse.statusCode.toInt(),
            headers = httpHeaders,
            message = "Failed to parse GraphQL http network response: EOF"
        )
    )

    return Result.Success(
        block(
            HttpResponse(
                statusCode = statusCode,
                headers = httpHeaders,
                body = Buffer().write(data.toByteString()))
        )
    )
  }

  sealed class Result<R> {
    class Success<R>(val response: R) : Result<R>()

    class Failure<R>(val cause: ApolloException) : Result<R>()
  }

  @Suppress("NAME_SHADOWING")
  private fun <R> Result<R>.dispatchOnMain(continuationPtr: COpaquePointer) {
    if (NSThread.isMainThread()) {
      dispatch(continuationPtr)
    } else {
      val continuationWithResultRef = StableRef.create((continuationPtr to this).freeze())
      dispatch_async_f(
          queue = dispatch_get_main_queue(),
          context = continuationWithResultRef.asCPointer(),
          work = staticCFunction { ptr ->
            val continuationWithResultRef = ptr!!.asStableRef<Pair<COpaquePointer, Result<R>>>()
            val (continuationPtr, result) = continuationWithResultRef.get()
            continuationWithResultRef.dispose()

            result.dispatch(continuationPtr)
          }
      )
    }
  }
}

@Suppress("NAME_SHADOWING")
@ExperimentalCoroutinesApi
internal fun <R> DefaultHttpEngine.Result<R>.dispatch(continuationPtr: COpaquePointer) {
  val continuationRef = continuationPtr.asStableRef<Continuation<DefaultHttpEngine.Result<R>>>()
  val continuation = continuationRef.get()
  continuationRef.dispose()

  continuation.resume(this)
}
