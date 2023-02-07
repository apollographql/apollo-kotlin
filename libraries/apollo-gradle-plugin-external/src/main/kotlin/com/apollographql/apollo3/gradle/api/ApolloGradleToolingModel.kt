package com.apollographql.apollo3.gradle.api

import java.io.File

interface ApolloGradleToolingModel {
  val version: Int

  val projectName: String
  val serviceInfos: List<ServiceInfo>

  interface ServiceInfo {
    val name: String
    val schemaFiles: Set<File>
    val graphqlSrcDirs: Set<File>
    val upstreamProjects: Set<String>
  }

  companion object {
    /**
     * Current version of the tooling model.
     * Increment this value when the model changes in breaking ways.
     * Adding properties / functions is never breaking, whereas deleting, renaming, changing types or signatures is breaking.
     */
    const val VERSION = 1
  }
}
