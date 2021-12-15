// This is in the `exception` package and not `api.exception` to keep some compatibility with 2.x
package com.apollographql.apollo3.exception

import com.apollographql.apollo3.api.http.HttpHeader
import okio.BufferedSource

/**
 * The base class for all exceptions
 *
 * This inherits from [RuntimeException]. Java callers will have to explicitly catch all [ApolloException]s.
 */
open class ApolloException(message: String? = null, cause: Throwable? = null) : RuntimeException(message, cause)

/**
 * A network error happened: socket closed, DNS issue, TLS problem, etc...
 *
 * @param message a message indicating what the error was.
 * @param platformCause the underlying cause. Might be null. When not null, it can be cast to:
 * - a [Throwable] on JVM platforms.
 * - a [NSError] on Darwin platforms.
 * to get more details about what went wrong.
 */
class ApolloNetworkException(
    message: String? = null,
    val platformCause: Any? = null,
) : ApolloException(message = message, cause = platformCause as? Throwable)

/**
 * A WebSocket connection could not be established: e.g., expired token
 */
class ApolloWebSocketClosedException(
    val code: Int,
    val reason: String? = null,
    cause: Throwable? = null,
) : ApolloException(message = "WebSocket Closed code='$code' reason='$reason'", cause = cause)

/**
 * The response was received but the response code was not 200
 *
 * @param statusCode: the HTTP status code
 * @param headers: the HTTP headers
 * @param body: the HTTP error body. By default, [body] is always null. You can opt-in [exposeHttpErrorBody] in [HttpNetworkTransport]
 * if you need it. If you're doing this, you **must** call [BufferedSource.close] on [body] to avoid sockets and other resources leaking.
 */
class ApolloHttpException(
    val statusCode: Int,
    val headers: List<HttpHeader>,
    val body: BufferedSource?,
    message: String,
    cause: Throwable? = null,
) : ApolloException(message = message, cause = cause)

/**
 * Thrown when the data being parsed is not encoded as valid JSON.
 */
class JsonEncodingException(message: String) : ApolloException(message)

/**
 * Thrown when the data in a JSON document doesn't match the data expected by the caller.
 *
 * For example, suppose the application expects a boolean but the JSON document contains a string. When the call to
 * [JsonReader.nextBoolean] is made, a `JsonDataException` is thrown.
 *
 * Exceptions of this type should be fixed by either changing the application code to accept the unexpected JSON, or by changing the JSON
 * to conform to the application's expectations.
 *
 * This exception may also be triggered if a document's nesting exceeds 31 levels. This depth is sufficient for all practical applications,
 * but shallow enough to avoid uglier failures like [StackOverflowError].
 */
class JsonDataException(message: String) : ApolloException(message)

/**
 * The response could not be parsed either because of another issue than [JsonDataException] or [JsonEncodingException]
 */
class ApolloParseException(message: String? = null, cause: Throwable? = null) : ApolloException(message = message, cause = cause)

/**
 * An object/field was missing in the cache
 * If [fieldName] is null, it means a reference to an object could not be resolved
 */
class CacheMissException(val key: String, val fieldName: String? = null) : ApolloException(message = message(key, fieldName)) {
  companion object {
    fun message(key: String?, fieldName: String?): String {
      return if (fieldName == null) {
        "Object '$key' not found"
      } else {
        "Object '$key' has no field named '$fieldName'"
      }
    }
  }
}

/**
 * A HTTP cache miss happened
 */
class HttpCacheMissException(message: String, cause: Exception? = null) : ApolloException(message = message, cause = cause)

/**
 * Multiple exceptions happened, for an exemple with a [CacheFirst] fetch policy
 */
class ApolloCompositeException(first: Throwable?, second: Throwable?) : ApolloException(message = "multiple exceptions happened", second) {
  val first = (first as? ApolloException) ?: throw RuntimeException("unexpected first exception", first)
  val second = (second as? ApolloException) ?: throw RuntimeException("unexpected second exception", second)
}

class AutoPersistedQueriesNotSupported : ApolloException(message = "The server does not support auto persisted queries")
class MissingValueException : ApolloException(message = "The optional doesn't have a value")

/**
 * Something went wrong but it's not sure exactly what
 */
@Deprecated("This is only used in the JVM runtime and is scheduled for removal")
class ApolloGenericException(message: String? = null, cause: Throwable? = null) : ApolloException(message = message, cause = cause)


@Deprecated("This is only used in the JVM runtime and is scheduled for removal")
class ApolloCanceledException(message: String? = null, cause: Throwable? = null) : ApolloException(message = message, cause = cause)



