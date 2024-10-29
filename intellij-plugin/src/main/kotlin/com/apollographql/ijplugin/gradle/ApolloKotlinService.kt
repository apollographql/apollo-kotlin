package com.apollographql.ijplugin.gradle

import com.intellij.util.messages.Topic
import com.intellij.util.xmlb.annotations.Attribute
import com.intellij.util.xmlb.annotations.Transient
import com.intellij.util.xmlb.annotations.XCollection

interface ApolloKotlinServiceListener {
  companion object {
    @Topic.ProjectLevel
    val TOPIC: Topic<ApolloKotlinServiceListener> =
      Topic.create("ApolloKotlinServices are available", ApolloKotlinServiceListener::class.java)
  }

  fun apolloKotlinServicesAvailable()
}

/**
 * Represents an Apollo Kotlin service as configured in the Apollo Gradle plugin configuration.
 *
 * These are built from the [com.apollographql.apollo.gradle.api.ApolloGradleToolingModel] and are used to configure the GraphQL plugin,
 * and are cached into the project settings.
 *
 * @see com.apollographql.ijplugin.gradle.GradleToolingModelService
 * @see com.apollographql.ijplugin.graphql.ApolloGraphQLConfigContributor
 * @see com.apollographql.ijplugin.settings.ProjectSettingsService
 */
data class ApolloKotlinService(
    @Attribute
    val gradleProjectPath: String = "",

    @Attribute
    val serviceName: String = "",

    @XCollection
    val schemaPaths: List<String> = emptyList(),

    @XCollection
    val operationPaths: List<String> = emptyList(),

    @Attribute
    val endpointUrl: String? = null,

    @XCollection
    val endpointHeaders: Map<String, String>? = null,

    @XCollection
    val upstreamServiceIds: List<Id> = emptyList(),

    @Attribute
    val useSemanticNaming: Boolean = true,
) {
  data class Id(
      @Attribute
      val gradleProjectPath: String = "",

      @Attribute
      val serviceName: String = "",
  ) {
    override fun toString(): String {
      return "$gradleProjectPath/$serviceName"
    }

    companion object {
      fun fromString(string: String): Id? {
        val split = string.split("/", limit = 2)
        if (split.size != 2) return null
        return Id(split[0], split[1])
      }
    }
  }

  val id: Id
    @Transient
    get() = Id(gradleProjectPath, serviceName)
}
