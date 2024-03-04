// This is in the `exception` package and not `api.exception` to keep some compatibility with 2.x
package com.apollographql.apollo3.exception

import com.apollographql.apollo3.annotations.ApolloExperimental
import com.apollographql.apollo3.annotations.ApolloInternal
import com.apollographql.apollo3.api.Error
import com.apollographql.apollo3.api.http.HttpHeader
import okio.BufferedSource

/**
 * The base class for all exceptions
 *
 * This inherits from [RuntimeException]. Java callers will have to explicitly catch all [ApolloException]s.
 */
sealed class ApolloException(message: String? = null, cause: Throwable? = null) : RuntimeException(message, cause)

/**
 * A generic exception when no additional context exists
 */
class DefaultApolloException(message: String? = null, cause: Throwable? = null): ApolloException(message, cause)

/**
 * No data was found
 */
class NoDataException(cause: Throwable?): ApolloException("No data was found", cause)

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
 * The server could not process a subscription and sent an error.
 *
 * This typically happens if there is a validation error.
 *
 * @param operationName the name of the subscription that triggered the error.
 * @param payload the payload returned by the server.
 */
class SubscriptionOperationException(
    operationName: String,
    val payload: Any?,
) : ApolloException(message = "Operation error $operationName")

class SubscriptionConnectionException(
    val payload: Any?,
) : ApolloException(message = "Websocket error")


/**
 * The router sent one or several errors.
 *
 * This exception is not always terminal. Other responses may follow
 *
 * @param errors a list of errors returned by the router
 */
class RouterError(
    val errors: List<Error>,
) : ApolloException(message = "Router error(s) (first: '${errors.firstOrNull()?.message}')")

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
 * @param statusCode the HTTP status code
 * @param headers the HTTP headers
 * @param body the HTTP error body. By default, [body] is always null. You can opt-in [HttpNetworkTransport.httpExposeErrorBody]
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

class ApolloGraphQLException(val error: Error): ApolloException("GraphQL error: '${error.message}'") {
  constructor(errors: List<Error>): this(errors.first())

  @Deprecated("Use error instead", level = DeprecationLevel.ERROR)
  val errors: List<Error> = listOf(error)
}

/**
 * An object/field was missing in the cache
 * If [fieldName] is null, it means a reference to an object could not be resolved
 */

class CacheMissException @ApolloInternal constructor(
    val key: String,
    val fieldName: String? = null,
    stale: Boolean = false,
) : ApolloException(message = message(key, fieldName, stale)) {

  @ApolloExperimental
  val stale: Boolean = stale

  constructor(key: String, fieldName: String?) : this(key, fieldName, false)

  companion object {
    internal fun message(key: String?, fieldName: String?, stale: Boolean): String {
      return if (fieldName == null) {
        "Object '$key' not found"
      } else {
        if (stale) {
          "Field '$fieldName' on object '$key' is stale"
        } else {
          "Object '$key' has no field named '$fieldName'"
        }
      }
    }
  }
}

/**
 * An HTTP cache miss happened
 */
class HttpCacheMissException(message: String, cause: Exception? = null) : ApolloException(message = message, cause = cause)

// See https://github.com/apollographql/apollo-kotlin/issues/4062
@Deprecated("ApolloCompositeException is deprecated. Handle each ApolloResponse.exception instead.")
class ApolloCompositeException : ApolloException {
  constructor(first: Throwable?, second: Throwable?) : super(message = "Multiple exceptions happened", second) {
    if (first != null) addSuppressed(first)
    if (second != null) addSuppressed(second)
  }

  constructor(exceptions: List<Throwable>) : super(message = "Multiple exceptions happened", exceptions.lastOrNull()) {
    exceptions.forEach { addSuppressed(it) }
  }
}

class AutoPersistedQueriesNotSupported : ApolloException(message = "The server does not support auto persisted queries")
class MissingValueException : ApolloException(message = "The optional doesn't have a value")
