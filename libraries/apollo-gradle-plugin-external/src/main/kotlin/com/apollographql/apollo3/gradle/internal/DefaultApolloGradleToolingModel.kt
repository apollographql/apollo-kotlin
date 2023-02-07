package com.apollographql.apollo3.gradle.internal

import com.apollographql.apollo3.gradle.api.ApolloGradleToolingModel
import java.io.File
import java.io.Serializable

internal data class DefaultApolloGradleToolingModel(
    override val projectName: String,
    override val serviceInfos: List<ApolloGradleToolingModel.ServiceInfo>,
) : ApolloGradleToolingModel, Serializable {
  override val version: Int = ApolloGradleToolingModel.VERSION
}

internal data class DefaultServiceInfo(
    override val name: String,
    override val schemaFiles: Set<File>,
    override val graphqlSrcDirs: Set<File>,
    override val upstreamProjects: Set<String>,

) : ApolloGradleToolingModel.ServiceInfo, Serializable
