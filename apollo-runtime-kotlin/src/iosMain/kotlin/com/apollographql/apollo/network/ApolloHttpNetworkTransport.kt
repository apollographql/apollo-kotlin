package com.apollographql.apollo.network

import com.apollographql.apollo.ApolloException
import com.apollographql.apollo.ApolloHttpException
import com.apollographql.apollo.ApolloNetworkException
import com.apollographql.apollo.api.ApolloExperimental
import com.apollographql.apollo.api.ExecutionContext
import com.apollographql.apollo.api.internal.json.JsonWriter
import kotlinx.cinterop.COpaquePointer
import kotlinx.cinterop.StableRef
import kotlinx.cinterop.asStableRef
import kotlinx.cinterop.staticCFunction
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
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
import platform.Foundation.NSURLComponents
import platform.Foundation.NSURLQueryItem
import platform.Foundation.NSURLRequest
import platform.Foundation.NSURLRequestReloadIgnoringCacheData
import platform.Foundation.NSURLResponse
import platform.Foundation.NSURLSession
import platform.Foundation.NSURLSessionDataTask
import platform.Foundation.dataTaskWithRequest
import platform.Foundation.setHTTPBody
import platform.Foundation.setHTTPMethod
import platform.Foundation.setValue
import platform.darwin.dispatch_async_f
import platform.darwin.dispatch_get_main_queue
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.native.concurrent.freeze

typealias UrlSessionDataTaskCompletionHandler = (NSData?, NSURLResponse?, NSError?) -> Unit
typealias UrlSessionDataTaskFactory = (NSURLRequest, UrlSessionDataTaskCompletionHandler) -> NSURLSessionDataTask

@ApolloExperimental
@ExperimentalCoroutinesApi
actual class ApolloHttpNetworkTransport(
    private val serverUrl: NSURL,
    private val headers: Map<String, String>,
    private val httpMethod: HttpMethod,
    private val dataTaskFactory: UrlSessionDataTaskFactory
) : NetworkTransport {

  actual constructor(
      serverUrl: String,
      headers: Map<String, String>,
      httpMethod: HttpMethod
  ) : this(
      serverUrl = NSURL(string = serverUrl),
      headers = headers,
      httpMethod = httpMethod,
      dataTaskFactory = { request, completionHandler ->
        NSURLSession.sharedSession.dataTaskWithRequest(request, completionHandler)
      }
  )

  override fun execute(request: GraphQLRequest, executionContext: ExecutionContext): Flow<GraphQLResponse> {
    return flow {
      assert(NSThread.isMainThread())

      val result = suspendCancellableCoroutine<Result> { continuation ->
        val continuationRef = StableRef.create(continuation).asCPointer()
        val delegate = { httpData: NSData?, httpResponse: NSURLResponse?, error: NSError? ->
          initRuntimeIfNeeded()
          val response = parse(
              data = httpData,
              httpResponse = httpResponse as? NSHTTPURLResponse,
              error = error
          )
          response.dispatchOnMain(continuationRef)
        }
        val httpRequest = request.toHttpRequest(executionContext[HttpExecutionContext.Request])

        dataTaskFactory(httpRequest.freeze(), delegate.freeze())
            .also { task ->
              continuation.invokeOnCancellation {
                task.cancel()
              }
            }
            .resume()
      }

      when (result) {
        is Result.Success -> emit(
            GraphQLResponse(
                body = Buffer().write(result.data.toByteString()),
                executionContext = HttpExecutionContext.Response(
                    statusCode = result.httpStatusCode,
                    headers = result.httpHeaders
                ),
                requestUuid = request.uuid
            )
        )
        is Result.Failure -> throw result.cause
      }
    }
  }

  private fun GraphQLRequest.toHttpRequest(httpExecutionContext: HttpExecutionContext.Request?): NSURLRequest {
    return when (httpMethod) {
      HttpMethod.Get -> toHttpGetRequest(httpExecutionContext)
      HttpMethod.Post -> toHttpPostRequest(httpExecutionContext)
    }
  }

  private fun GraphQLRequest.toHttpGetRequest(httpExecutionContext: HttpExecutionContext.Request?): NSURLRequest {
    val urlComponents = NSURLComponents(uRL = serverUrl, resolvingAgainstBaseURL = false)
    urlComponents.queryItems = listOfNotNull(
        NSURLQueryItem(name = "query", value = document),
        NSURLQueryItem(name = "operationName", value = operationName),
        if (variables.isNotEmpty()) NSURLQueryItem(name = "variables", value = variables) else null
    )
    return NSMutableURLRequest.requestWithURL(urlComponents.URL!!).apply {
      setHTTPMethod("GET")
      headers
          .plus(httpExecutionContext?.headers ?: emptyMap())
          .forEach { (key, value) -> setValue(value, forHTTPHeaderField = key) }
      setCachePolicy(NSURLRequestReloadIgnoringCacheData)
    }
  }

  private fun GraphQLRequest.toHttpPostRequest(httpExecutionContext: HttpExecutionContext.Request?): NSURLRequest {
    return NSMutableURLRequest.requestWithURL(serverUrl).apply {
      val buffer = Buffer()
      JsonWriter.of(buffer)
          .beginObject()
          .name("operationName").value(operationName)
          .name("query").value(document)
          .name("variables").jsonValue(variables)
          .endObject()
          .close()
      val postBody = buffer.readByteArray().toNSData()

      setHTTPMethod("POST")
      headers
          .plus(httpExecutionContext?.headers ?: emptyMap())
          .forEach { (key, value) -> setValue(value, forHTTPHeaderField = key) }
      setCachePolicy(NSURLRequestReloadIgnoringCacheData)
      setHTTPBody(postBody)
    }
  }

  private fun parse(
      data: NSData?,
      httpResponse: NSHTTPURLResponse?,
      error: NSError?
  ): Result {
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
    if (statusCode !in 200..299) return Result.Failure(
        cause = ApolloHttpException(
            statusCode = httpResponse.statusCode.toInt(),
            headers = httpHeaders,
            message = "Http request failed with status code `$statusCode`"
        )
    )

    if (data == null) return Result.Failure(
        cause = ApolloHttpException(
            statusCode = httpResponse.statusCode.toInt(),
            headers = httpHeaders,
            message = "Failed to parse GraphQL http network response: EOF"
        )
    )

    return Result.Success(
        data = data,
        httpStatusCode = httpResponse.statusCode.toInt(),
        httpHeaders = httpHeaders
    )
  }

  sealed class Result {
    class Success(val data: NSData, val httpStatusCode: Int, val httpHeaders: Map<String, String>) : Result()
    class Failure(val cause: ApolloException) : Result()
  }

  @Suppress("NAME_SHADOWING")
  private fun Result.dispatchOnMain(continuationPtr: COpaquePointer) {
    if (NSThread.isMainThread()) {
      dispatch(continuationPtr)
    } else {
      val continuationWithResultRef = StableRef.create((continuationPtr to this).freeze())
      dispatch_async_f(
          queue = dispatch_get_main_queue(),
          context = continuationWithResultRef.asCPointer(),
          work = staticCFunction { ptr ->
            val continuationWithResultRef = ptr!!.asStableRef<Pair<COpaquePointer, Result>>()
            val (continuationPtr, result) = continuationWithResultRef.get()
            continuationWithResultRef.dispose()

            result.dispatch(continuationPtr)
          }
      )
    }
  }
}

@Suppress("NAME_SHADOWING")
@ApolloExperimental
@ExperimentalCoroutinesApi
internal fun ApolloHttpNetworkTransport.Result.dispatch(continuationPtr: COpaquePointer) {
  val continuationRef = continuationPtr.asStableRef<Continuation<ApolloHttpNetworkTransport.Result>>()
  val continuation = continuationRef.get()
  continuationRef.dispose()

  continuation.resume(this)
}
