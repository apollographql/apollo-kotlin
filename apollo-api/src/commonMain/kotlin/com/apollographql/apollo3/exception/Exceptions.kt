package com.apollographql.apollo3.exception

/**
 * The base class for all exceptions
 */
open class ApolloException(message: String? = null, cause: Throwable? = null) : RuntimeException(message, cause)

/**
 * A network error happened: socket closed, DNS issue, TLS problem, etc...
 */
class ApolloNetworkException(message: String? = null, cause: Throwable? = null) : ApolloException(message = message, cause = cause)

/**
 * A WebSocket connection could not be established: e.g., expired token
 */
class ApolloWebSocketClosedException(
    val code: Int,
    message: String? = null,
    cause: Throwable? = null) : ApolloException(message = message, cause = cause)

/**
 * The response was received but the response code was not 200
 */
class ApolloHttpException(
    val statusCode: Int,
    val headers: Map<String, String>,
    message: String,
    cause: Throwable? = null
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
  private companion object {
    private fun message(key: String?, fieldName: String?): String {
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
  val first = (first as? ApolloException)  ?: throw RuntimeException("unexpected first exception", first)
  val second = (second as? ApolloException)  ?: throw RuntimeException("unexpected second exception", second)
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



