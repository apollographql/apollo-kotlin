package com.apollographql.apollo.api

interface InputType {
  fun marshaller(): InputFieldMarshaller
}
