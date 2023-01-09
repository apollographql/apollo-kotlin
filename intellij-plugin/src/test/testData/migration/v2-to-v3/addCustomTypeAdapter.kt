package com.example

import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.api.CustomTypeAdapter
import com.apollographql.apollo.api.CustomTypeValue
import com.apollographql.apollo.api.ScalarType
import java.net.URL
import java.util.*

enum class CustomType : ScalarType {
  DATETIME {
    override fun typeName(): String = "DateTime"
    override fun className(): String = "kotlin.Any"
  },

  URL {
    override fun typeName(): String = "Url"
    override fun className(): String = "kotlin.Any"
  }
}

suspend fun main() {
  val apolloClient = ApolloClient.builder()
    .addCustomTypeAdapter(CustomType.DATETIME, DateTimeAdapter())
    .addCustomTypeAdapter(CustomType.URL, UrlAdapter())
    .build()
}

class DateTimeAdapter : CustomTypeAdapter<Date> {
  override fun decode(value: CustomTypeValue<*>): Date {
    TODO("Not yet implemented")
  }

  override fun encode(value: Date): CustomTypeValue<*> {
    TODO("Not yet implemented")
  }
}

class UrlAdapter : CustomTypeAdapter<URL> {
  override fun decode(value: CustomTypeValue<*>): URL {
    TODO("Not yet implemented")
  }

  override fun encode(value: URL): CustomTypeValue<*> {
    TODO("Not yet implemented")
  }
}
