package com.apollographql.apollo.gradle.task

import com.apollographql.apollo.compiler.model.ProjectModel
import com.apollographql.apollo.compiler.model.writeTo
import com.apollographql.apollo.tooling.model.TelemetryData
import com.apollographql.apollo.tooling.model.writeTo
import gratatouille.tasks.GOutputFile
import gratatouille.tasks.GTask

@GTask
internal fun apolloGenerateProjectModel(
    // Inputs
    serviceNames: Set<String>,
    apolloTasksDependencies: Set<String>,

    // Telemetry data
    gradleVersion: String?,
    androidMinSdk: Int?,
    androidTargetSdk: Int?,
    androidCompileSdk: String?,
    androidAgpVersion: String?,
    apolloGenerateSourcesDuringGradleSync: Boolean?,
    apolloLinkSqlite: Boolean?,
    usedServiceOptions: Set<String>,

    // Outputs
    projectModel: GOutputFile,
    telemetryData: GOutputFile,
) {
  ProjectModel(
      serviceNames = serviceNames,
      apolloTasksDependencies = apolloTasksDependencies,
  )
      .writeTo(projectModel)

  TelemetryData(
      gradleVersion = gradleVersion,
      androidMinSdk = androidMinSdk,
      androidTargetSdk = androidTargetSdk,
      androidCompileSdk = androidCompileSdk,
      androidAgpVersion = androidAgpVersion,
      apolloGenerateSourcesDuringGradleSync = apolloGenerateSourcesDuringGradleSync,
      apolloLinkSqlite = apolloLinkSqlite,
      usedServiceOptions = usedServiceOptions,
  )
      .writeTo(telemetryData)
}
