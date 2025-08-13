package com.apollographql.apollo.gradle.task

import com.apollographql.apollo.compiler.model.ProjectModel
import com.apollographql.apollo.compiler.model.writeTo
import gratatouille.tasks.GOutputFile
import gratatouille.tasks.GTask

@GTask
internal fun apolloGenerateProjectModel(
    // Inputs
    serviceNames: Set<String>,

    gradleVersion: String?,
    androidMinSdk: Int?,
    androidTargetSdk: Int?,
    androidCompileSdk: String?,
    androidAgpVersion: String?,
    apolloGenerateSourcesDuringGradleSync: Boolean?,
    apolloLinkSqlite: Boolean?,

    // Outputs
    projectModelFile: GOutputFile,
) {
  ProjectModel(
      serviceNames = serviceNames,

      gradleVersion = gradleVersion,
      androidMinSdk = androidMinSdk,
      androidTargetSdk = androidTargetSdk,
      androidCompileSdk = androidCompileSdk,
      androidAgpVersion = androidAgpVersion,
      apolloGenerateSourcesDuringGradleSync = apolloGenerateSourcesDuringGradleSync,
      apolloLinkSqlite = apolloLinkSqlite,
  )
      .writeTo(projectModelFile)
}
