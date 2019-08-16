package com.apollographql.apollo.api

import java.io.IOException

interface InputFieldMarshaller {
  @Throws(IOException::class)
  fun marshal(writer: InputFieldWriter)
}
