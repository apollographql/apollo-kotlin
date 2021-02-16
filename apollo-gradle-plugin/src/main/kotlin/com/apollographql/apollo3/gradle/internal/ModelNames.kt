package com.apollographql.apollo3.gradle.internal

import com.apollographql.apollo3.gradle.api.Service

object ModelNames {
  private fun camelCase(vararg elements: String): String {
    return elements.mapIndexed { index, s ->
      if (index != 0) {
        s.capitalize()
      } else {
        s
      }
    }.joinToString("")
  }

  // Task names
  fun generateApolloSources() = "generateApolloSources"
  fun generateApolloSources(service: Service) = camelCase("generate", service.name, "ApolloSources")
  fun downloadApolloSchema() = camelCase("downloadApolloSchema")
  fun downloadApolloSchemaIntrospection(service: Service) = camelCase("download", service.name, "ApolloSchemaFromIntrospection")
  fun downloadApolloSchemaRegistry(service: Service) = camelCase("download", service.name, "ApolloSchemaFromRegistry")
  fun pushApolloSchema() = camelCase("pushApolloSchema")
  fun checkApolloVersions() = "checkApolloVersions"
  fun checkApolloDuplicates(service: Service) = camelCase("check", service.name, "ApolloDuplicates")
  fun convertApolloSchema() = "convertApolloSchema"

  // Configuration names
  fun apolloConfiguration() = "apolloMetadata" // not just 'apollo' to avoid name clashing with the apollo {} extension
  fun producerConfiguration(service: Service) = camelCase("apollo", service.name, "Producer")
  fun consumerConfiguration(service: Service) = camelCase("apollo", service.name, "Consumer")
  fun duplicatesConsumerConfiguration(service: Service) = camelCase("apollo", service.name, "DuplicatesConsumer")
}