package com.apollographql.apollo.kmpsample.data

import com.apollographql.apollo.api.Operation
import com.apollographql.apollo.api.Response
import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.COpaquePointer
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.StableRef
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.asStableRef
import kotlinx.cinterop.convert
import kotlinx.cinterop.get
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.staticCFunction
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.suspendCancellableCoroutine
import okio.Buffer
import okio.IOException
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
import kotlin.native.concurrent.DetachedObjectGraph
import kotlin.native.concurrent.attach
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
        val payload = data!!.toByteArray()
        val response = parse(Buffer().write(payload))
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

  private fun NSData.toByteArray(): ByteArray {
    val data: CPointer<ByteVar> = bytes!!.reinterpret()
    return ByteArray(length.toInt()) { index -> data[index] }
  }

  @Suppress("NAME_SHADOWING")
  private fun <T : Operation.Data> Result<T>.dispatchOnMain(continuationRef: COpaquePointer) {
    dispatch_async_f(
        queue = dispatch_get_main_queue(),
        context = DetachedObjectGraph { (continuationRef to this).freeze() }.asCPointer(),
        work = staticCFunction { it ->
          val continuationRefAndResponse = DetachedObjectGraph<Pair<COpaquePointer, Result<T>>>(it).attach()
          val continuationRef = continuationRefAndResponse.first.asStableRef<CancellableContinuation<Response<T>>>()
          val continuation = continuationRef.get()
          continuationRef.dispose()

          when (val response = continuationRefAndResponse.second) {
            is Result.Success -> continuation.resume(response.value)
            is Result.Failure -> continuation.resumeWithException(response.error)
          }
        }
    )
  }
}
