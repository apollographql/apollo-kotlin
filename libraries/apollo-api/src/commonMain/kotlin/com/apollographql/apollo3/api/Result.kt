package com.apollographql.apollo3.api

import com.apollographql.apollo3.api.json.JsonReader
import com.apollographql.apollo3.exception.ApolloGraphQLException
import com.apollographql.apollo3.exception.DefaultApolloException

sealed interface Result<out V> {
  fun getOrNull(): V? = (this as? Data)?.value
  fun getOrThrow(): V {
    return when(this) {
      is Data -> value
      is Error -> throw ApolloGraphQLException(errors)
    }
  }

  fun errorsOrNull(): List<com.apollographql.apollo3.api.Error>? = (this as? Error)?.errors

  class Data<V>(val value: V): Result<V>
  class Error(val errors: List<com.apollographql.apollo3.api.Error>) : Result<Nothing>
}


fun <V> Result<V>.getOrElse(fallback: V): V = (this as? Result.Data)?.value ?: fallback

fun <V> missingFieldResult(jsonReader: JsonReader, adapterContext: CompositeAdapterContext): Result<V> {
  val errors = adapterContext.errorsForPath(jsonReader.getPath())
  if (errors.isEmpty()) {
    throw DefaultApolloException("No field returned at ${jsonReader.getPath()}")
  }

  return Result.Error(errors)
}

fun <V> missingField(jsonReader: JsonReader): V {
  throw DefaultApolloException("No field returned at ${jsonReader.getPath()}")
}