package com.apollographql.apollo.api

import com.apollographql.apollo.api.internal.InputFieldMarshaller

interface InputType {

  fun marshaller(): InputFieldMarshaller
}
