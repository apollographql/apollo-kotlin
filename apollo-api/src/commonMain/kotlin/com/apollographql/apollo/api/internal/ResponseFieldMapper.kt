package com.apollographql.apollo.api.internal

import kotlin.jvm.JvmSynthetic

/**
 * ResponseFieldMapper is an abstraction for mapping the response data returned by
 * the server back to generated models.
 */
fun interface ResponseFieldMapper<T> {
  fun map(responseReader: ResponseReader): T

  companion object {
    @JvmSynthetic
    inline operator fun <T> invoke(crossinline block: (ResponseReader) -> T) =
        ResponseFieldMapper { responseReader -> block(responseReader) }
  }
}
