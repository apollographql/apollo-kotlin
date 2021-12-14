package com.apollographql.apollo3.gradle.api

import org.gradle.api.Named
import org.gradle.api.attributes.Attribute

object ApolloAttributes {
  interface Service : Named {
  }

  val APOLLO_SERVICE_ATTRIBUTE = Attribute.of("com.apollographql.service", Service::class.java)
}