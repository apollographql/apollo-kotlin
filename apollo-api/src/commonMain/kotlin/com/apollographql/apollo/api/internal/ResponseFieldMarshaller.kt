package com.apollographql.apollo.api.internal

import kotlin.jvm.JvmSynthetic

fun interface ResponseFieldMarshaller {
  fun marshal(writer: ResponseWriter)

  companion object {
    @JvmSynthetic
    inline operator fun invoke(crossinline block: (ResponseWriter) -> Unit) =
        ResponseFieldMarshaller { writer -> block(writer) }
  }
}
