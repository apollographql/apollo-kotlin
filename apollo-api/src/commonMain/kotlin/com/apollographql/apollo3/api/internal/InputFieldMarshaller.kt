package com.apollographql.apollo.api.internal

import okio.IOException
import kotlin.jvm.JvmSynthetic

interface InputFieldMarshaller {
  @Throws(IOException::class)
  fun marshal(writer: InputFieldWriter)

  companion object {
    @JvmSynthetic
    inline operator fun invoke(crossinline block: (InputFieldWriter) -> Unit) = object : InputFieldMarshaller {
      override fun marshal(writer: InputFieldWriter) {
        block(writer)
      }
    }
  }
}
