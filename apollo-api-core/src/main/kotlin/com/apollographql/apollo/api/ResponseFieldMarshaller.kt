package com.apollographql.apollo.api

interface ResponseFieldMarshaller {

  fun marshal(writer: ResponseWriter)

  companion object {
    inline operator fun invoke(crossinline block: (writer: ResponseWriter) -> Unit) = object : ResponseFieldMarshaller {
      override fun marshal(writer: ResponseWriter) {
        block(writer)
      }
    }
  }
}
