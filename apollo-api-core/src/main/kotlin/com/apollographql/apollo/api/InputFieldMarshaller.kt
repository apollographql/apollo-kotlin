package com.apollographql.apollo.api

import java.io.IOException

interface InputFieldMarshaller {

  @Throws(IOException::class)
  fun marshal(writer: InputFieldWriter)

  companion object {
    @JvmSynthetic
    inline operator fun invoke(crossinline block: (writer: InputFieldWriter) -> Unit) = object : InputFieldMarshaller {
      override fun marshal(writer: InputFieldWriter) {
        block(writer)
      }
    }
  }
}
