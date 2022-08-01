package com.apollographql.apollo3.internal

import com.apollographql.apollo3.api.http.HttpResponse
import com.apollographql.apollo3.api.http.valueOf
import com.apollographql.apollo3.exception.ApolloException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onCompletion
import okio.BufferedSource

/**
 * Create a Flow of a body that is sent in parts, when the response is in a multipart content type.
 *
 * This Flow can be collected only once, and the parts must be consumed as they go.
 * The [response] is automatically closed after the last emission.
 * Closing the individual parts in the Flow doesn't close the overall response.
 */
internal fun multipartBodyFlow(response: HttpResponse): Flow<BufferedSource> {
  var multipartReader: MultipartReader? = null
  return flow {
    multipartReader = MultipartReader(
        source = response.body!!,
        boundary = getBoundaryParameter(response.headers.valueOf("Content-Type"))
            ?: throw ApolloException("Expected the Content-Type to have a boundary parameter")
    )
    while (true) {
      val part = multipartReader!!.nextPart() ?: break
      emit(part.body)
    }
  }.onCompletion {
    runCatching { multipartReader?.close() }
  }
}

/**
 * Example inputs:
 * - `multipart/mixed; boundary="-"`
 * - `multipart/mixed; boundary=boundary`
 */
private fun getBoundaryParameter(contentType: String?): String? {
  if (contentType == null) return null
  val parameters = contentType.split(';').map { it.trim() }
  val boundaryParameter = parameters.firstOrNull { it.startsWith("boundary=") }
  return boundaryParameter?.split('=')?.getOrNull(1)?.trim('"', '\'')
}

internal val HttpResponse.isMultipart: Boolean
  get() = headers.valueOf("Content-Type")?.startsWith("multipart/", ignoreCase = true) == true
