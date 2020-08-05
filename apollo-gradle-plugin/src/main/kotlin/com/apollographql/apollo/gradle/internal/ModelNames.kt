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
  fun zipMetadata(compilationUnit: DefaultCompilationUnit) =  camelCase("zip", compilationUnit.variantName, compilationUnit.serviceName, "ApolloMetadata")
  fun downloadApolloSchema() = camelCase("downloadApolloSchema")
  fun downloadApolloSchema(service: DefaultService) = camelCase("download", service.name, "ApolloSchema")
  fun checkApolloVersions() = "checkApolloVersions"

  // Configuration names
  fun consumerConfiguration() = "apollo"
  fun producerConfiguration(compilationUnit: DefaultCompilationUnit) = camelCase("apollo", compilationUnit.variantName, compilationUnit.serviceName, "Producer")
  fun consumerConfiguration(compilationUnit: DefaultCompilationUnit) = camelCase("apollo", compilationUnit.variantName, compilationUnit.serviceName, "Consumer")
}