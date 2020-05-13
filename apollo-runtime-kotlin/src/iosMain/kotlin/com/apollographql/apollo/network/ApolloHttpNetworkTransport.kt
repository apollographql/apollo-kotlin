package com.apollographql.apollo.network

import com.apollographql.apollo.ApolloError
import com.apollographql.apollo.ApolloException
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

typealias DataTaskCompletionHandler = (NSData?, NSURLResponse?, NSError?) -> Unit
typealias DataTaskProvider = (NSURLRequest, DataTaskCompletionHandler) -> NSURLSessionDataTask

@ApolloExperimental
@ExperimentalCoroutinesApi
actual class ApolloHttpNetworkTransport(
    private val serverUrl: NSURL,
    private val httpHeaders: Map<String, String>,
    private val httpMethod: HttpMethod,
    private val dataTaskProvider: DataTaskProvider
) : NetworkTransport {

  actual constructor(
      serverUrl: String,
      httpHeaders: Map<String, String>,
      httpMethod: HttpMethod
  ) : this(
      serverUrl = NSURL(string = serverUrl),
      httpHeaders = httpHeaders,
      httpMethod = httpMethod,
      dataTaskProvider = { request, completionHandler -> NSURLSession.sharedSession.dataTaskWithRequest(request, completionHandler) }
  )

  override fun execute(request: GraphQLRequest): Flow<GraphQLResponse> {
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
        val httpRequest = request.toHttpRequest()

        dataTaskProvider(httpRequest.freeze(), delegate.freeze())
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
                executionContext = ExecutionContext.Empty
            )
        )
        is Result.Failure -> throw result.cause
      }
    }
  }

  private fun GraphQLRequest.toHttpRequest(): NSURLRequest {
    return when (httpMethod) {
      HttpMethod.Get -> toHttpGetRequest()
      HttpMethod.Post -> toHttpPostRequest()
    }
  }

  private fun GraphQLRequest.toHttpGetRequest(): NSURLRequest {
    val urlComponents = NSURLComponents(uRL = serverUrl, resolvingAgainstBaseURL = false)
    urlComponents.queryItems = listOfNotNull(
        NSURLQueryItem(name = "query", value = document),
        NSURLQueryItem(name = "operationName", value = operationName),
        if (variables.isNotEmpty()) NSURLQueryItem(name = "variables", value = variables) else null
    )
    return NSMutableURLRequest.requestWithURL(urlComponents.URL!!).apply {
      setHTTPMethod("GET")
      httpHeaders.forEach { (key, value) -> setValue(value, forHTTPHeaderField = key) }
      setCachePolicy(NSURLRequestReloadIgnoringCacheData)
    }
  }

  private fun GraphQLRequest.toHttpPostRequest(): NSURLRequest {
    return NSMutableURLRequest.requestWithURL(serverUrl).apply {
      val buffer = Buffer()
      JsonWriter.of(buffer)
          .beginObject()
          .name("operationName").value(operationName)
          .name("query").value(document)
          .name("variables").value(variables)
          .endObject()
          .close()
      val postBody = buffer.readByteArray().toNSData()

      setHTTPMethod("POST")
      httpHeaders.forEach { (key, value) -> setValue(value, forHTTPHeaderField = key) }
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
        ApolloException(
            message = "Failed to execute GraphQL http network request",
            error = ApolloError.Network,
            cause = IOException(error.localizedDescription)
        )
    )

    if (httpResponse == null) return Result.Failure(
        ApolloException(
            message = "Failed to parse GraphQL http network response: EOF",
            error = ApolloError.Network
        )
    )

    val statusCode = httpResponse.statusCode.toInt()
    if (statusCode !in 200..299) return Result.Failure(
        ApolloException(
            message = "Http request failed with status code `$statusCode`",
            error = ApolloError.Network
        )
    )

    if (data == null) return Result.Failure(
        ApolloException(
            message = "Failed to parse GraphQL http network response: EOF",
            error = ApolloError.Network
        )
    )

    return Result.Success(data)
  }

  sealed class Result {
    class Success(val data: NSData) : Result()
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
