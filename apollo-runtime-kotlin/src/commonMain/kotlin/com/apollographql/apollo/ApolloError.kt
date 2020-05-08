package com.apollographql.apollo

sealed class ApolloError {
  abstract val message: String

  class SerializationError(override val message: String) : ApolloError()

  class ParseError(override val message: String) : ApolloError()

  class Network(override val message: String) : ApolloError()
}
