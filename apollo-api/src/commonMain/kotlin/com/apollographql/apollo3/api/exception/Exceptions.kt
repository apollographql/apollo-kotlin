package com.apollographql.apollo3.api.exception

/**
 * The base class for all exceptions
 */
open class ApolloException(message: String? = null, cause: Throwable? = null) : RuntimeException(message, cause)

/**
 * A network happened: socket closed, DNS issue, TLS problem, etc...
 */
class ApolloNetworkException(message: String? = null, cause: Throwable? = null) : ApolloException(message = message, cause = cause)

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
 * The request body could not be generated to be sent to the server
 */
class ApolloSerializationException(message: String? = null, cause: Throwable? = null) : ApolloException(message = message, cause = cause)

/**
 * The response could not be parsed either because:
 * - the return json is not valid
 * - or it doesn't contain
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
class ApolloGenericException(message: String? = null, cause: Throwable? = null) : ApolloException(message = message, cause = cause)

class ApolloBearerTokenException(message: String, cause: Throwable? = null, val token: String): ApolloException(message = message, cause = cause)

@Deprecated("This is only used in the JVM runtime and is schedule for removal")
class ApolloCanceledException(message: String? = null, cause: Throwable? = null) : ApolloException(message = message, cause = cause)



