package com.apollographql.apollo3.api

import com.apollographql.apollo3.exception.ApolloGraphQLException

sealed interface FieldResult<out V> {
  class Success<V>(val value: V) : FieldResult<V>
  class Error(val error: com.apollographql.apollo3.api.Error) : FieldResult<Nothing>
}

fun <V> FieldResult<V>.getOrElse(fallback: V): V = if (this is FieldResult.Success) value else fallback
val <V> FieldResult<V>.isSuccess: Boolean get() = this is FieldResult.Success
val <V> FieldResult<V>.valueOrNull: V? get() = if (this is FieldResult.Success) value else null
val <V> FieldResult<V>.errorsOrNull: Error? get() = if (this is FieldResult.Error) error else null

fun <V> FieldResult<V>.valueOrThrow(): V {
  return when (this) {
    is FieldResult.Success -> value
    is FieldResult.Error -> throw ApolloGraphQLException(error)
  }
}
