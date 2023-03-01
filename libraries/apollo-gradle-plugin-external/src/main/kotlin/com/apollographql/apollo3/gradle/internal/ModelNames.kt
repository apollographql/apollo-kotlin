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
  fun generateApolloSourcesFromIr(service: Service) = camelCase("generate", service.name, "ApolloSourcesFromIr")
  fun generateApolloSchema(service: Service) = camelCase("generate", service.name, "ApolloSchema")
  fun generateApolloIr(service: Service) = camelCase("generate", service.name, "ApolloIr")
  fun generateApolloUsedCoordinates(service: Service) = camelCase("generate", service.name, "ApolloUsedCoordinates")
  fun downloadApolloSchema() = camelCase("downloadApolloSchema")
  fun downloadApolloSchemaIntrospection(service: Service) = camelCase("download", service.name, "ApolloSchemaFromIntrospection")
  fun downloadApolloSchemaRegistry(service: Service) = camelCase("download", service.name, "ApolloSchemaFromRegistry")
  fun registerApolloOperations(service: DefaultService) = camelCase("register", service.name, "ApolloOperations")
  fun pushApolloSchema() = camelCase("pushApolloSchema")
  fun checkApolloVersions() = "checkApolloVersions"
  fun convertApolloSchema() = "convertApolloSchema"

  // Configuration names
  fun metadataConfiguration() = "apolloMetadata"
  fun metadataProducerConfiguration(service: Service) = camelCase("apollo", service.name, "Producer")
  fun metadataConsumerConfiguration(service: Service) = camelCase("apollo", service.name, "Consumer")
  fun upstreamIrProducerConfiguration(service: Service) = camelCase("apollo", service.name, "UpstreamIrProducer")
  fun upstreamIrConsumerConfiguration(service: Service) = camelCase("apollo", service.name, "UpstreamIrConsumer")
  fun downstreamIrProducerConfiguration(service: Service) = camelCase("apollo", service.name, "DownStreamIrProducer")
  fun downstreamIrConsumerConfiguration(service: Service) = camelCase("apollo", service.name, "DownStreamIrConsumer")
  fun schemaProducerConfiguration(service: Service) = camelCase("apollo", service.name, "SchemaProducer")
  fun schemaConsumerConfiguration(service: Service) = camelCase("apollo", service.name, "SchemaConsumer")
}
