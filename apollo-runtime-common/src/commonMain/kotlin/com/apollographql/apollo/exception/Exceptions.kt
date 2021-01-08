package com.apollographql.apollo.exception

// TODO: Make this a sealed class
abstract class ApolloException(message: String? = null, cause: Throwable? = null) : RuntimeException(message, cause)

class ApolloSerializationException(message: String? = null, cause: Throwable? = null) : ApolloException(message = message, cause = cause)

class ApolloParseException(message: String? = null, cause: Throwable? = null) : ApolloException(message = message, cause = cause)

class ApolloCanceledException(message: String? = null, cause: Throwable? = null) : ApolloException(message = message, cause = cause)

class ApolloNetworkException(message: String? = null, cause: Throwable? = null) : ApolloException(message = message, cause = cause)

class ApolloGenericException(message: String? = null, cause: Throwable? = null) : ApolloException(message = message, cause = cause)

class ApolloHttpException(
    val statusCode: Int,
    val headers: Map<String, String>,
    message: String,
    cause: Throwable? = null
) : ApolloException(message = message, cause = cause)

class ApolloBearerTokenException(message: String, cause: Throwable? = null, val token: String): ApolloException(message = message, cause = cause)

class ApolloWebSocketException(message: String, cause: Throwable? = null) : ApolloException(message = message, cause = cause)

class ApolloWebSocketServerException(message: String, val payload: Map<String, Any?>) : ApolloException(message = message, cause = null)
