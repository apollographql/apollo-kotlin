package com.apollographql.apollo.gradle.internal

import com.apollographql.apollo.compiler.capitalizeFirstLetter
import com.apollographql.apollo.gradle.api.Service

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
  fun generateApolloCodegenSchema(service: Service) = camelCase("generate", service.name, "ApolloCodegenSchema")
  fun generateApolloIrOperations(service: Service) = camelCase("generate", service.name, "ApolloIrOperations")
  fun generateApolloOptions(service: Service) = camelCase("generate", service.name, "ApolloOptions")
  fun downloadApolloSchema() = camelCase("downloadApolloSchema")
  fun downloadApolloSchemaIntrospection(service: Service) = camelCase("download", service.name, "ApolloSchemaFromIntrospection")
  fun downloadApolloSchemaRegistry(service: Service) = camelCase("download", service.name, "ApolloSchemaFromRegistry")
  fun registerApolloOperations(service: DefaultService) = camelCase("register", service.name, "ApolloOperations")
  fun pushApolloSchema() = camelCase("pushApolloSchema")
  fun checkApolloVersions() = "checkApolloVersions"
  fun convertApolloSchema() = "convertApolloSchema"

  // Configuration names
  fun metadataConfiguration() = "apolloMetadata"
  fun codegenMetadataProducerConfiguration(service: Service) = camelCase("apollo", service.name, "CodegenMetadataProducer")
  fun codegenMetadataConsumerConfiguration(service: Service) = camelCase("apollo", service.name, "CodegenMetadataConsumer")
  fun upstreamIrProducerConfiguration(service: Service) = camelCase("apollo", service.name, "UpstreamIrProducer")
  fun upstreamIrConsumerConfiguration(service: Service) = camelCase("apollo", service.name, "UpstreamIrConsumer")
  fun downstreamIrProducerConfiguration(service: Service) = camelCase("apollo", service.name, "DownStreamIrProducer")
  fun downstreamIrConsumerConfiguration(serviceName: String) = camelCase("apollo", serviceName, "DownStreamIrConsumer")
  fun codegenSchemaProducerConfiguration(service: Service) = camelCase("apollo", service.name, "CodegenSchemaProducer")
  fun codegenSchemaConsumerConfiguration(service: Service) = camelCase("apollo", service.name, "CodegenSchemaConsumer")
  fun otherOptionsProducerConfiguration(service: Service) = camelCase("apollo", service.name, "OtherOptionsProducer")
  fun otherOptionsConsumerConfiguration(service: Service) = camelCase("apollo", service.name, "OtherOptionsConsumer")
  fun compilerConfiguration(service: DefaultService) = camelCase("apollo", service.name, "Compiler")
 }
