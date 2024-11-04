// This is in the `exception` package and not `api.exception` to keep some compatibility with 2.x
package com.apollographql.apollo.exception

import com.apollographql.apollo.annotations.ApolloExperimental
import com.apollographql.apollo.annotations.ApolloInternal
import com.apollographql.apollo.api.Error
import com.apollographql.apollo.api.http.HttpHeader
import okio.BufferedSource
import okio.IOException

/**
 * The base class for all exceptions
 *
 * This inherits from [RuntimeException]. Java callers will have to explicitly catch all [ApolloException]s.
 */
sealed class ApolloException(message: String? = null, cause: Throwable? = null) : RuntimeException(message, cause)

/**
 * A generic exception used when there is no additional context besides the message.
 */
class DefaultApolloException(message: String? = null, cause: Throwable? = null) : ApolloException(message, cause)

/**
 * No data was found
 */
class NoDataException(cause: Throwable?) : ApolloException("No data was found", cause)

/**
 * An I/O error happened: socket closed, DNS issue, TLS problem, file not found, etc...
 *
 * This is called [ApolloNetworkException] for historical reasons, but it should have been `ApolloIOException` instead.
 * [ApolloNetworkException] is thrown when an I/O error happens reading the operation.
 *
 * @param message a message indicating what the error was.
 * @param platformCause the underlying cause to get more details about what went wrong. When not null, it is either:
 * - a [Throwable]
 * - or a [NSError] on Apple platforms.
 */
class ApolloNetworkException(
    message: String? = null,
    val platformCause: Any? = null,
) : ApolloException(message = message, cause = platformCause as? Throwable)

/**
 * The device has been detected as offline
 */
data object OfflineException: IOException("The device is offline")

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

/**
 * The server responded with an error to the 'connection_init' message.
 */
class SubscriptionConnectionException(
    val payload: Any?,
) : ApolloException(message = "Subscription connection error")

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
 * A WebSocket close frame was received from the server
 */
class ApolloWebSocketClosedException(
    val code: Int,
    val reason: String? = null,
    cause: Throwable? = null,
) : ApolloException(message = "WebSocket Closed code='$code' reason='$reason'", cause = cause)

/**
 * `closeConnection()` was called to force closing the websocket
 */
@ApolloExperimental
data object ApolloWebSocketForceCloseException : ApolloException(message = "closeConnection() was called")

/**
 * The response was received but the media type was `application/json` and the code was not 2xx.
 * Note that `application/graphql-response+json` do not throw this exception.
 *
 * See https://graphql.github.io/graphql-over-http/draft/.
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
 */
class JsonDataException(message: String) : ApolloException(message)

/**
 * A field was missing or null in the JSON response.
 *
 * Due to the way the parsers work, it is not possible to distinguish between both cases.
 */
class NullOrMissingField(message: String) : ApolloException(message)

/**
 * The response could not be parsed because of an I/O exception.
 *
 * JSON and GraphQL errors are throwing other errors, see [JsonEncodingException], [JsonDataException] and [NullOrMissingField]
 *
 * @see JsonEncodingException
 * @see JsonDataException
 * @see NullOrMissingField
 */
@Deprecated("ApolloParseException was only used for I/O exceptions and is now mapped to ApolloNetworkException.")
class ApolloParseException(message: String? = null, cause: Throwable? = null) : ApolloException(message = message, cause = cause)

class ApolloGraphQLException(val error: Error) : ApolloException("GraphQL error: '${error.message}'") {
  constructor(errors: List<Error>) : this(errors.first())

  @Deprecated("Use error instead", level = DeprecationLevel.ERROR)
  val errors: List<Error> = listOf(error)
}

/**
 * An object/field was missing in the cache
 * If [fieldName] is null, it means a reference to an object could not be resolved
 */
class CacheMissException @ApolloInternal constructor(
    /**
     * The cache key to the missing object, or to the parent of the missing field if [fieldName] is not null.
     */
    val key: String,

    /**
     * The field key that was missing. If null, it means the object referenced by [key] was missing.
     */
    val fieldName: String? = null,

    stale: Boolean = false,
) : ApolloException(message = message(key, fieldName, stale)) {

  @ApolloExperimental
  val stale: Boolean = stale

  constructor(key: String, fieldName: String?) : this(key, fieldName, false)

  companion object {
    internal fun message(cacheKey: String?, fieldKey: String?, stale: Boolean): String {
      return if (fieldKey == null) {
        "Object '$cacheKey' not found"
      } else {
        if (stale) {
          "Field '$fieldKey' on object '$cacheKey' is stale"
        } else {
          "Object '$cacheKey' has no field named '$fieldKey'"
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
