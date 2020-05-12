package com.apollographql.apollo.network

import com.apollographql.apollo.ApolloError
import com.apollographql.apollo.ApolloException
import com.apollographql.apollo.api.ApolloExperimental
import com.apollographql.apollo.api.ExecutionContext
import com.apollographql.apollo.api.internal.json.JsonWriter
import kotlinx.cinterop.COpaquePointer
import kotlinx.cinterop.StableRef
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.asStableRef
import kotlinx.cinterop.convert
import kotlinx.cinterop.staticCFunction
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.ProducerScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import okio.Buffer
import okio.IOException
import okio.toByteString
import platform.Foundation.NSData
import platform.Foundation.NSError
import platform.Foundation.NSHTTPURLResponse
import platform.Foundation.NSMutableData
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
import platform.Foundation.appendBytes
import platform.Foundation.dataTaskWithRequest
import platform.Foundation.setHTTPBody
import platform.Foundation.setHTTPMethod
import platform.Foundation.setValue
import platform.darwin.dispatch_async_f
import platform.darwin.dispatch_get_main_queue
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
    return callbackFlow {
      assert(NSThread.isMainThread())

      val producerRef = StableRef.create(this).asCPointer()
      val delegate = { httpData: NSData?, httpResponse: NSURLResponse?, error: NSError? ->
        initRuntimeIfNeeded()
        val response = parse(
            data = httpData,
            httpResponse = httpResponse as? NSHTTPURLResponse,
            error = error
        )
        response.dispatchOnMain(producerRef)
      }
      val httpRequest = request.toHttpRequest()

      val task = dataTaskProvider(httpRequest.freeze(), delegate.freeze()).apply {
        resume()
      }
      awaitClose {
        task.cancel()
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
  private fun Result.dispatchOnMain(producerRef: COpaquePointer) {
    if (NSThread.isMainThread()) {
      dispatch(producerRef)
    } else {
      val producerWithResultRef = StableRef.create((producerRef to this).freeze())
      dispatch_async_f(
          queue = dispatch_get_main_queue(),
          context = producerWithResultRef.asCPointer(),
          work = staticCFunction { ref ->
            val producerWithResultRef = ref!!.asStableRef<Pair<COpaquePointer, Result>>()
            val (producerRef, result) = producerWithResultRef.get()
            producerWithResultRef.dispose()

            result.dispatch(producerRef)
          }
      )
    }
  }
}


@Suppress("NAME_SHADOWING")
@ApolloExperimental
@ExperimentalCoroutinesApi
internal fun ApolloHttpNetworkTransport.Result.dispatch(producerRef: COpaquePointer) {
  val producerRef = producerRef.asStableRef<ProducerScope<GraphQLResponse>>()
  val producer = producerRef.get()
  producerRef.dispose()

  when (this) {
    is ApolloHttpNetworkTransport.Result.Success -> {
      producer.offer(
          GraphQLResponse(
              body = Buffer().write(data.toByteString()),
              executionContext = ExecutionContext.Empty
          )
      )
      producer.close()
    }
    is ApolloHttpNetworkTransport.Result.Failure -> {
      producer.cancel(
          message = cause.message,
          cause = cause
      )
    }
  }
}
