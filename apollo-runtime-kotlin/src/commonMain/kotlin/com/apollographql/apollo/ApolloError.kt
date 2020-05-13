package com.apollographql.apollo

sealed class ApolloError {
  object SerializationError : ApolloError()
  object ParseError : ApolloError()
  object Network : ApolloError()
}
