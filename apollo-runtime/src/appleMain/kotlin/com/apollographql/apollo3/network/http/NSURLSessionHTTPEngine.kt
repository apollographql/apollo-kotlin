package com.apollographql.apollo3.network.http

import com.apollographql.apollo3.api.http.HttpHeader
import com.apollographql.apollo3.api.http.HttpMethod
import com.apollographql.apollo3.api.http.HttpRequest
import com.apollographql.apollo3.api.http.HttpResponse
import com.apollographql.apollo3.exception.ApolloNetworkException
import com.apollographql.apollo3.mpp.assertMainThreadOnNative
import com.apollographql.apollo3.mpp.suspendAndResumeOnMain
import com.apollographql.apollo3.network.toNSData
import okio.Buffer
import okio.toByteString
import platform.Foundation.NSData
import platform.Foundation.NSError
import platform.Foundation.NSHTTPURLResponse
import platform.Foundation.NSMutableURLRequest
import platform.Foundation.NSURL
import platform.Foundation.NSURLRequest
import platform.Foundation.NSURLRequestReloadIgnoringCacheData
import platform.Foundation.NSURLResponse
import platform.Foundation.NSURLSession
import platform.Foundation.NSURLSessionConfiguration
import platform.Foundation.NSURLSessionDataTask
import platform.Foundation.dataTaskWithRequest
import platform.Foundation.setHTTPBody
import platform.Foundation.setHTTPMethod
import platform.Foundation.setValue
import kotlin.native.concurrent.freeze

typealias UrlSessionDataTaskCompletionHandler = (NSData?, NSURLResponse?, NSError?) -> Unit

fun interface DataTaskFactory {
  fun dataTask(request: NSURLRequest, completionHandler: UrlSessionDataTaskCompletionHandler): NSURLSessionDataTask
}

actual class MultiplatformHttpEngine(
    private val dataTaskFactory: DataTaskFactory,
    private val connectTimeoutMillis: Long = 60_000,
) : HttpEngine {

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
  override suspend fun execute(request: HttpRequest) = suspendAndResumeOnMain<HttpResponse> { mainContinuation, invokeOnCancellation ->
    assertMainThreadOnNative()

    request.freeze()

    val delegate = { httpData: NSData?, nsUrlResponse: NSURLResponse?, error: NSError? ->
      initRuntimeIfNeeded()

      mainContinuation.resumeWith(
          buildHttpResponse(
              data = httpData,
              httpResponse = nsUrlResponse as? NSHTTPURLResponse,
              error = error,
          )
      )
    }

    val nsMutableURLRequest = NSMutableURLRequest.requestWithURL(
        URL = NSURL(string = request.url)
    ).apply {
      setTimeoutInterval(connectTimeoutMillis.toDouble() / 1000)

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

    val task = dataTaskFactory.dataTask(nsMutableURLRequest.freeze(), delegate)
    invokeOnCancellation {
      task.cancel()
    }
    delegate.freeze()
    task.resume()
  }

  override fun dispose() {
  }
}

@OptIn(ExperimentalUnsignedTypes::class)
private fun buildHttpResponse(
    data: NSData?,
    httpResponse: NSHTTPURLResponse?,
    error: NSError?,
): Result<HttpResponse> {

  if (error != null) {
    return Result.failure(
        ApolloNetworkException(
            message = "Failed to execute GraphQL http network request",
            platformCause = error.freeze()
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

  /**
   * data can be empty if there is no body.
   * In that case, trying to create a ByteString fails
   */
  val bodyString = if (data == null || data.length.toInt() == 0) {
    null
  } else {
    data.toByteString()
  }

  return Result.success(
      HttpResponse(
          statusCode = statusCode,
          headers = httpHeaders,
          bodyString = bodyString,
          bodySource = null
      )
  )
}

