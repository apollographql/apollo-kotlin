package com.apollographql.apollo3.api

import com.apollographql.apollo3.api.internal.InputFieldMarshaller

interface InputType {

  fun marshaller(): InputFieldMarshaller
}
