package com.apollographql.apollo

sealed class ApolloException(message: String, cause: Throwable?) : RuntimeException(message, cause)

class ApolloSerializationException(message: String, cause: Throwable? = null) : ApolloException(message = message, cause = cause)

class ApolloParseException(message: String, cause: Throwable? = null) : ApolloException(message = message, cause = cause)

class ApolloNetworkException(message: String, cause: Throwable? = null) : ApolloException(message = message, cause = cause)

class ApolloHttpException(
    val statusCode: Int,
    val headers: Map<String, String>,
    message: String,
    cause: Throwable? = null
) : ApolloException(message = message, cause = cause)

class BearerTokenException(message: String, cause: Throwable? = null, val token: String): ApolloException(message = message, cause = cause)
