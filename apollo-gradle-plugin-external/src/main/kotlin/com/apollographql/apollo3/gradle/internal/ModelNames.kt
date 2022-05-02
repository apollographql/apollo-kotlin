package com.apollographql.apollo3.gradle.internal

import com.apollographql.apollo3.compiler.capitalizeFirstLetter
import com.apollographql.apollo3.gradle.api.Service

object ModelNames {
  private fun camelCase(vararg elements: String): String {
    return elements.mapIndexed { index, s ->
      if (index != 0) {
        s.capitalizeFirstLetter()
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
  fun registerApolloOperations(service: DefaultService) = camelCase("register", service.name, "ApolloOperations")
  fun pushApolloSchema() = camelCase("pushApolloSchema")
  fun checkApolloVersions() = "checkApolloVersions"
  fun checkApolloDuplicates(service: Service) = camelCase("check", service.name, "ApolloDuplicates")
  fun convertApolloSchema() = "convertApolloSchema"

  // Configuration names. See https://docs.gradle.org/current/userguide/declaring_dependencies.html#sec:resolvable-consumable-configs
  /**
   * A bucket configuration
   */
  fun apolloConfiguration() = "apolloMetadata" // not just 'apollo' to avoid name clashing with the apollo {} extension
  fun producerConfiguration(service: Service) = camelCase("apollo", service.name, "Producer")
  fun consumerConfiguration(service: Service) = camelCase("apollo", service.name, "Consumer")

  /**
   * A resolvable configuration that will collect all metadata for a given service name
   */
  fun duplicatesConsumerConfiguration(service: Service) = camelCase("apollo", service.name, "DuplicatesConsumer")
}