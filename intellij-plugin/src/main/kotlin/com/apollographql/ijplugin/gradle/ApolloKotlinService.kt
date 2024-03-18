package com.apollographql.ijplugin.gradle

import com.intellij.util.messages.Topic

interface ApolloKotlinServiceListener {
  companion object {
    @Topic.ProjectLevel
    val TOPIC: Topic<ApolloKotlinServiceListener> = Topic.create("ApolloKotlinServices are available", ApolloKotlinServiceListener::class.java)
  }

  fun apolloKotlinServicesAvailable()
}

data class ApolloKotlinService(
    val gradleProjectPath: String,
    val serviceName: String,
    val schemaPaths: List<String>,
    val operationPaths: List<String>,
    val endpointUrl: String?,
    val endpointHeaders: Map<String, String>?,
) {
  data class Id(val gradleProjectPath: String, val serviceName: String) {
    override fun toString(): String {
      val formattedPath = gradleProjectPath.split(":").filterNot { it.isEmpty() }.joinToString("-")
      return "$formattedPath/$serviceName"
    }

    companion object {
      fun fromString(string: String): Id? {
        val split = string.split("/")
        if (split.size != 2) return null
        return Id(split[0], split[1])
      }
    }
  }

  val id = Id(gradleProjectPath, serviceName)
}
