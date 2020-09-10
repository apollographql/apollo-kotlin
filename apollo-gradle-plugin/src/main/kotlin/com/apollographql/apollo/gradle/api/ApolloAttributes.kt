package com.apollographql.apollo.gradle.api

import org.gradle.api.Named
import org.gradle.api.attributes.Attribute

object ApolloAttributes {
  interface Variant : Named {
  }

  val APOLLO_VARIANT_ATTRIBUTE = Attribute.of("com.apollographql.variant", Variant::class.java)

  interface Service : Named {
  }

  val APOLLO_SERVICE_ATTRIBUTE = Attribute.of("com.apollographql.service", Service::class.java)
}