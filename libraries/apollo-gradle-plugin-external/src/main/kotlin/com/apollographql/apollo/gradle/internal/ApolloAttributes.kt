package com.apollographql.apollo.gradle.internal

import org.gradle.api.attributes.Attribute

internal val APOLLO_SERVICE_ATTRIBUTE = Attribute.of("com.apollographql.service", String::class.java)
internal val APOLLO_DIRECTION_ATTRIBUTE = Attribute.of("com.apollographql.direction", String::class.java)

internal enum class ApolloUsage {
  CodegenSchema,
  OtherOptions,
  Ir,
  CodegenMetadata
}

internal enum class ApolloDirection {
  Upstream,
  Downstream
}

internal enum class ConfigurationKind {
  DependencyScope,
  Consumable,
  Resolvable
}