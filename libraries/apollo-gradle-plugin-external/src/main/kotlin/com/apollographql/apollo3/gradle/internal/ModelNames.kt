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
  fun generateApolloSchema(service: Service) = camelCase("generate", service.name, "ApolloSchema")
  fun generateApolloUsedCoordinates(service: Service) = camelCase("generate", service.name, "ApolloUsedCoordinates")
  fun generateApolloUsedCoordinatesAggregate(service: Service) = camelCase("generate", service.name, "ApolloUsedCoordinatesAggregate")
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
  fun metadataConfiguration() = "apolloMetadata" // not just 'apollo' to avoid name clashing with the apollo {} extension
  fun usedCoordinatesConfiguration() = "apolloUsedCoordinates" // not just 'apollo' to avoid name clashing with the apollo {} extension
  fun schemaConfiguration() = "apolloSchema" // not just 'apollo' to avoid name clashing with the apollo {} extension
  fun metadataProducerConfiguration(service: Service) = camelCase("apollo", service.name, "Producer")
  fun metadataConsumerConfiguration(service: Service) = camelCase("apollo", service.name, "Consumer")
  fun usedCoordinatesProducerConfiguration(service: Service) = camelCase("apollo", service.name, "UsedCoordinatesProducer")
  fun usedCoordinatesConsumerConfiguration(service: Service) = camelCase("apollo", service.name, "UsedCoordinatesConsumer")
  fun schemaProducerConfiguration(service: Service) = camelCase("apollo", service.name, "SchemaProducer")
  fun schemaConsumerConfiguration(service: Service) = camelCase("apollo", service.name, "SchemaConsumer")

  /**
   * A resolvable configuration that will collect all metadata for a given service name
   */
  fun duplicatesConsumerConfiguration(service: Service) = camelCase("apollo", service.name, "DuplicatesConsumer")
}