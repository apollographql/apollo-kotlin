package com.apollographql.apollo3.api

import com.apollographql.apollo3.exception.ApolloException
import com.apollographql.apollo3.exception.ApolloGraphQLException
import com.apollographql.apollo3.exception.DefaultApolloException

sealed interface FieldResult<out V> {
  class Success<V>(val value: V) : FieldResult<V>
  class Failure(val exception: ApolloException) : FieldResult<Nothing>
}

val <V> FieldResult<V>.isSuccess: Boolean get() = this is FieldResult.Success
fun <V> FieldResult<V>.getOrElse(fallback: V): V = if (this is FieldResult.Success) value else fallback
fun <V> FieldResult<V>.valueOrNull(): V? = if (this is FieldResult.Success) value else null
fun <V> FieldResult<V>.exceptionOrNull(): Exception? = if (this is FieldResult.Failure) exception else null
fun <V> FieldResult<V>.graphQLErrorOrNull(): Error? = (exceptionOrNull() as? ApolloGraphQLException)?.error

fun <V> FieldResult<V>.valueOrThrow(): V {
  return when (this) {
    is FieldResult.Success -> value
    is FieldResult.Failure -> throw DefaultApolloException("Field error", exception)
  }
}
