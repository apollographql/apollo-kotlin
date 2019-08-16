package com.apollographql.apollo.api

interface ResponseFieldMarshaller {
  fun marshal(writer: ResponseWriter)
}
