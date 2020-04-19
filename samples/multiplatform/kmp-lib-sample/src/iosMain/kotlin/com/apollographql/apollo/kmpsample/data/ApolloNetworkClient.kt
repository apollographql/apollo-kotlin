package com.apollographql.apollo.kmpsample.data

import com.apollographql.apollo.api.Operation
import com.apollographql.apollo.api.Response
import kotlinx.cinterop.COpaquePointer
import kotlinx.cinterop.StableRef
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.asStableRef
import kotlinx.cinterop.convert
import kotlinx.cinterop.staticCFunction
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.suspendCancellableCoroutine
import okio.IOException
import okio.toByteString
import platform.Foundation.NSData
import platform.Foundation.NSError
import platform.Foundation.NSHTTPURLResponse
import platform.Foundation.NSMutableData
import platform.Foundation.NSMutableURLRequest
import platform.Foundation.NSThread
import platform.Foundation.NSURL
import platform.Foundation.NSURLRequestReloadIgnoringCacheData
import platform.Foundation.NSURLResponse
import platform.Foundation.NSURLSession
import platform.Foundation.NSURLSessionConfiguration
import platform.Foundation.appendBytes
import platform.Foundation.dataTaskWithRequest
import platform.Foundation.setHTTPBody
import platform.Foundation.setHTTPMethod
import platform.Foundation.setValue
import platform.darwin.dispatch_async_f
import platform.darwin.dispatch_get_main_queue
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.native.concurrent.freeze

internal class ApolloNetworkClient(
    private val url: String,
    private val headers: Map<String, String> = emptyMap()
) {

  suspend fun <T : Operation.Data> Operation<*, T, *>.send(): Response<T> {
    val session = NSURLSession.sessionWithConfiguration(NSURLSessionConfiguration.defaultSessionConfiguration())
    val request = prepareRequest(url = url, headers = headers)
    return suspendCancellableCoroutine { continuation ->
      assert(NSThread.isMainThread())
      val continuationRef = StableRef.create(continuation).asCPointer()
      try {
        val delegate = { httpData: NSData?, httpResponse: NSURLResponse?, error: NSError? ->
          initRuntimeIfNeeded()
          val response = parse(
              data = httpData,
              httpResponse = httpResponse as NSHTTPURLResponse,
              error = error
          )
          response.dispatchOnMain(continuationRef)
        }
        session.dataTaskWithRequest(request.freeze(), delegate.freeze()).resume()
      } catch (e: Exception) {
        continuationRef.asStableRef<CancellableContinuation<Response<T>>>().dispose()
        continuation.resumeWithException(e)
      }
    }
  }

  private fun <D : Operation.Data> Operation<D, *, *>.prepareRequest(
      url: String,
      headers: Map<String, String> = emptyMap()
  ): NSMutableURLRequest {
    return NSMutableURLRequest.requestWithURL(NSURL(string = url)).apply {
      setHTTPMethod("POST")
      setCachePolicy(NSURLRequestReloadIgnoringCacheData)
      headers.forEach { (key, value) -> setValue(value, forHTTPHeaderField = key) }
      setValue(operationId(), forHTTPHeaderField = "X-APOLLO-OPERATION-ID")
      setValue(name().name(), forHTTPHeaderField = "X-APOLLO-OPERATION-NAME")
      setHTTPBody(composeRequestBody().toByteArray().toNSData())
    }
  }

  private fun <T : Operation.Data> Operation<*, T, *>.parse(
      data: NSData?,
      httpResponse: NSHTTPURLResponse,
      error: NSError?
  ): Result<T> {
    if (error != null) {
      return Result.Failure(IOException(error.localizedDescription))
    }

    return try {
      val statusCode = httpResponse.statusCode.toInt()
      if (statusCode in 200..299) {
        // Here is the successful Response parsing happening
        val response = parse(data!!.toByteString())
        Result.Success(response)
      } else {
        Result.Failure(IOException("Network request failed, HTTP status code `$statusCode`"))
      }
    } catch (e: Exception) {
      Result.Failure(e)
    }
  }

  private sealed class Result<T : Operation.Data> {
    class Success<T : Operation.Data>(val value: Response<T>) : Result<T>()
    class Failure<T : Operation.Data>(val error: Exception) : Result<T>()
  }

  private fun ByteArray.toNSData(): NSData = NSMutableData().apply {
    if (isEmpty()) return@apply
    this@toNSData.usePinned {
      appendBytes(it.addressOf(0), size.convert())
    }
  }

  @Suppress("NAME_SHADOWING")
  private fun <T : Operation.Data> Result<T>.dispatchOnMain(continuationRef: COpaquePointer) {
    val continuationWithResultRef = StableRef.create((continuationRef to this).freeze())
    dispatch_async_f(
        queue = dispatch_get_main_queue(),
        context = continuationWithResultRef.asCPointer(),
        work = staticCFunction { ref ->
          val continuationWithResultRef = ref!!.asStableRef<Pair<COpaquePointer, Result<T>>>()
          val (continuationPointer, result) = continuationWithResultRef.get()
          continuationWithResultRef.dispose()

          val continuationRef = continuationPointer.asStableRef<CancellableContinuation<Response<T>>>()
          val continuation = continuationRef.get()
          continuationRef.dispose()

          when (result) {
            is Result.Success -> continuation.resume(result.value)
            is Result.Failure -> continuation.resumeWithException(result.error)
          }
        }
    )
  }
}
