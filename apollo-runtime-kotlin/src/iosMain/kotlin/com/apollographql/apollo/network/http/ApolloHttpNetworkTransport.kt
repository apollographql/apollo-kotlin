package com.apollographql.apollo.network.http

import com.apollographql.apollo.ApolloException
import com.apollographql.apollo.ApolloHttpException
import com.apollographql.apollo.ApolloNetworkException
import com.apollographql.apollo.ApolloParseException
import com.apollographql.apollo.ApolloSerializationException
import com.apollographql.apollo.api.ApolloExperimental
import com.apollographql.apollo.api.ExecutionContext
import com.apollographql.apollo.api.Operation
import com.apollographql.apollo.interceptor.ApolloRequest
import com.apollographql.apollo.interceptor.ApolloResponse
import com.apollographql.apollo.network.HttpExecutionContext
import com.apollographql.apollo.network.HttpMethod
import com.apollographql.apollo.network.NetworkTransport
import com.apollographql.apollo.network.toNSData
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
import platform.Foundation.NSURLSessionConfiguration
import platform.Foundation.NSURLSessionDataTask
import platform.Foundation.dataTaskWithRequest
import platform.Foundation.setHTTPBody
import platform.Foundation.setHTTPMethod
import platform.Foundation.setValue
import platform.darwin.dispatch_async_f
import platform.darwin.dispatch_get_main_queue
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.native.concurrent.freeze

typealias UrlSessionDataTaskCompletionHandler = (NSData?, NSURLResponse?, NSError?) -> Unit

fun interface DataTaskFactory {
  fun dataTask(request: NSURLRequest, completionHandler: UrlSessionDataTaskCompletionHandler): NSURLSessionDataTask
}

@ApolloExperimental
@ExperimentalCoroutinesApi
actual class ApolloHttpNetworkTransport(
    private val serverUrl: NSURL,
    private val headers: Map<String, String>,
    private val httpMethod: HttpMethod,
    private val dataTaskFactory: DataTaskFactory,
    private val connectTimeoutMillis: Long = 60_000,
) : NetworkTransport {

  actual constructor(
      serverUrl: String,
      headers: Map<String, String>,
      httpMethod: HttpMethod,
      connectTimeoutMillis: Long,
      readTimeoutMillis: Long,
  ) : this(
      serverUrl = NSURL(string = serverUrl),
      headers = headers,
      httpMethod = httpMethod,
      dataTaskFactory = DefaultDataTaskFactory(readTimeoutMillis),
      connectTimeoutMillis = connectTimeoutMillis,
  )

  private class DefaultDataTaskFactory(readTimeoutMillis: Long) : DataTaskFactory {
    private val nsurlSession = NSURLSession.sessionWithConfiguration(NSURLSessionConfiguration.defaultSessionConfiguration().apply {
      timeoutIntervalForRequest = readTimeoutMillis.toDouble()/1000
    })

    override fun dataTask(request: NSURLRequest, completionHandler: UrlSessionDataTaskCompletionHandler): NSURLSessionDataTask {
      return nsurlSession.dataTaskWithRequest(request, completionHandler)
    }

  }
  @Suppress("UNCHECKED_CAST")
  override fun <D : Operation.Data> execute(request: ApolloRequest<D>, executionContext: ExecutionContext): Flow<ApolloResponse<D>> {
    return flow {
      assert(NSThread.isMainThread())

      request.freeze()

      val result = suspendCancellableCoroutine<Result> { continuation ->
        val continuationRef = StableRef.create(continuation).asCPointer()
        val delegate = { httpData: NSData?, httpResponse: NSURLResponse?, error: NSError? ->
          initRuntimeIfNeeded()

          parse(
              request = request,
              data = httpData,
              httpResponse = httpResponse as? NSHTTPURLResponse,
              error = error
          ).dispatchOnMain(continuationRef)
        }

        val httpRequest = try {
          request.toHttpRequest(executionContext[HttpExecutionContext.Request])
        } catch (e: Exception) {
          continuation.resumeWithException(
              ApolloSerializationException(
                  message = "Failed to compose GraphQL network request",
                  cause = e
              )
          )
          return@suspendCancellableCoroutine
        }

        dataTaskFactory.dataTask(httpRequest.freeze(), delegate.freeze())
            .also { task ->
              continuation.invokeOnCancellation {
                task.cancel()
              }
            }
            .resume()
      }

      when (result) {
        is Result.Success -> emit(result.response as ApolloResponse<D>)
        is Result.Failure -> throw result.cause
      }
    }
  }

  private fun ApolloRequest<*>.toHttpRequest(httpExecutionContext: HttpExecutionContext.Request?): NSURLRequest {
    return when (httpMethod) {
      HttpMethod.Get -> toHttpGetRequest(httpExecutionContext)
      HttpMethod.Post -> toHttpPostRequest(httpExecutionContext)
    }
  }

  private fun ApolloRequest<*>.toHttpGetRequest(httpExecutionContext: HttpExecutionContext.Request?): NSURLRequest {
    val urlComponents = NSURLComponents(uRL = serverUrl, resolvingAgainstBaseURL = false)
    urlComponents.queryItems = listOfNotNull(
        NSURLQueryItem(name = "query", value = operation.queryDocument()),
        NSURLQueryItem(name = "operationName", value = operation.name().name()),
        operation.variables().marshal(customScalarAdapters).let { variables ->
          if (variables.isNotEmpty()) NSURLQueryItem(name = "variables", value = variables) else null
        }
    )
    return NSMutableURLRequest.requestWithURL(
        URL = urlComponents.URL!!
    ).apply {
      setHTTPMethod("GET")
      setTimeoutInterval(connectTimeoutMillis.toDouble() / 1000)
      headers
          .plus(httpExecutionContext?.headers ?: emptyMap())
          .forEach { (key, value) -> setValue(value, forHTTPHeaderField = key) }
      setCachePolicy(NSURLRequestReloadIgnoringCacheData)
    }
  }

  private fun ApolloRequest<*>.toHttpPostRequest(httpExecutionContext: HttpExecutionContext.Request?): NSURLRequest {
    return NSMutableURLRequest.requestWithURL(serverUrl).apply {
      val postBody = operation.composeRequestBody(customScalarAdapters).toByteArray().toNSData()
      setHTTPMethod("POST")
      headers
          .plus("Content-Type" to "application/json; charset=utf-8")
          .plus(httpExecutionContext?.headers ?: emptyMap())
          .forEach { (key, value) -> setValue(value, forHTTPHeaderField = key) }
      setCachePolicy(NSURLRequestReloadIgnoringCacheData)
      setHTTPBody(postBody)
    }
  }

  private fun <D : Operation.Data> parse(
      request: ApolloRequest<D>,
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

    return try {
      val response = request.operation.parse(
          source = Buffer().write(data.toByteString()).apply { flush() },
          customScalarAdapters = request.customScalarAdapters
      )
      Result.Success(
          ApolloResponse<D>(
              requestUuid = request.requestUuid,
              response = response,
              executionContext = request.executionContext + HttpExecutionContext.Response(
                  statusCode = httpResponse.statusCode.toInt(),
                  headers = httpHeaders
              )
          )
      )
    } catch (e: Exception) {
      Result.Failure(
          cause = ApolloParseException(
              message = "Failed to parse GraphQL network response",
              cause = e
          )
      )
    }
  }

  sealed class Result {
    class Success(val response: ApolloResponse<*>) : Result()

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
