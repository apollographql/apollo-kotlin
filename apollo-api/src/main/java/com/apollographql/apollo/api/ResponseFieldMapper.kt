package com.apollographql.apollo.api

/**
 * ResponseFieldMapper is an abstraction for mapping the response data returned by
 * the server back to generated models.
 */
interface ResponseFieldMapper<T> {
  fun map(responseReader: ResponseReader): T
}
