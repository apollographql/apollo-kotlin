package com.apollographql.apollo3.exception

open class ApolloException(message: String? = null, cause: Throwable? = null) : RuntimeException(message, cause)

class ApolloSerializationException(message: String? = null, cause: Throwable? = null) : ApolloException(message = message, cause = cause)

class ApolloParseException(message: String? = null, cause: Throwable? = null) : ApolloException(message = message, cause = cause)
class UnexpectedNullValue(fieldName: String) : ApolloException(message = "Unexpected null value at '$fieldName'")

class ObjectMissingException(key: String? = null) : ApolloException(message = "Object '$key' is not cached")
class FieldMissingException(key: String? = null, fieldName: String, canonicalFieldName: String) : ApolloException(message = "Object '$key' has no field named '$fieldName' ($canonicalFieldName)")

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
