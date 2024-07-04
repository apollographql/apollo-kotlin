package com.apollographql.apollo.gradle.internal

import com.apollographql.apollo.annotations.ApolloDeprecatedSince
import com.apollographql.apollo.compiler.capitalizeFirstLetter
import com.apollographql.apollo.gradle.api.Service

internal object ModelNames {
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
  fun registerApolloOperations(service: Service) = camelCase("register", service.name, "ApolloOperations")
  fun pushApolloSchema() = camelCase("pushApolloSchema")
  fun checkApolloVersions() = "checkApolloVersions"
  fun convertApolloSchema() = "convertApolloSchema"

  // Configuration names
  @Deprecated("Unused. Use dependsOn() instead.")
  @ApolloDeprecatedSince(ApolloDeprecatedSince.Version.v4_0_0)
  fun metadataConfiguration() = "apolloMetadata"
  fun configuration(serviceName: String, apolloDirection: ApolloDirection, apolloUsage: ApolloUsage, configurationKind: ConfigurationKind): String {
    return camelCase(
        "apollo",
        serviceName,
        apolloDirection.pretty(),
        apolloUsage.name,
        configurationKind.pretty()
    )
  }
  fun compilerConfiguration(service: Service) = camelCase("apollo", service.name, "Compiler")
 }

private fun ConfigurationKind.pretty(): String {
  return when(this) {
    ConfigurationKind.DependencyScope -> ""
    else -> name
  }
}


private fun ApolloDirection.pretty(): String {
  return when(this) {
    ApolloDirection.Upstream -> ""
    ApolloDirection.Downstream -> name
  }
}
