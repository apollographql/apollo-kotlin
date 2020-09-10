package com.apollographql.apollo.gradle.internal

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
  fun generateApolloSources(apolloVariant: ApolloVariant) = camelCase("generate", apolloVariant.name, "ApolloSources")
  fun generateApolloSources(compilationUnit: DefaultCompilationUnit) = camelCase("generate", compilationUnit.variantName, compilationUnit.serviceName, "ApolloSources")
  fun downloadApolloSchema() = camelCase("downloadApolloSchema")
  fun downloadApolloSchema(service: DefaultService) = camelCase("download", service.name, "ApolloSchema")
  fun checkApolloVersions() = "checkApolloVersions"
  fun checkApolloDuplicates(compilationUnit: DefaultCompilationUnit)= camelCase("check", compilationUnit.variantName, compilationUnit.serviceName, "ApolloDuplicates")

  // Configuration names
  fun apolloConfiguration() = "apolloMetadata" // not just 'apollo' to avoid name clashing with the apollo {} extension
  fun producerConfiguration(compilationUnit: DefaultCompilationUnit) = camelCase("apollo", compilationUnit.variantName, compilationUnit.serviceName, "Producer")
  fun consumerConfiguration(compilationUnit: DefaultCompilationUnit) = camelCase("apollo", compilationUnit.variantName, compilationUnit.serviceName, "Consumer")
  fun duplicatesConsumerConfiguration(compilationUnit: DefaultCompilationUnit) = camelCase("apollo", compilationUnit.variantName, compilationUnit.serviceName, "DuplicatesConsumer")
}