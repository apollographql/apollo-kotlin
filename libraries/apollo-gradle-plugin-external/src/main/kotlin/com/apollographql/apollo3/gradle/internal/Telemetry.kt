package com.apollographql.apollo3.gradle.internal

import org.gradle.api.Project

internal fun getTelemetryData(project: Project, apolloExtension: DefaultApolloExtension) = DefaultTelemetryData(
    gradleVersion = project.gradle.gradleVersion,
    androidMinSdk = project.androidExtension?.minSdk,
    androidTargetSdk = project.androidExtension?.targetSdk,
    androidCompileSdk = project.androidExtension?.compileSdkVersion,
    androidAgpVersion = agpVersion,
    apolloServiceTelemetryData = apolloExtension.getServiceTelemetryData(),
    apolloGenerateSourcesDuringGradleSync = apolloExtension.generateSourcesDuringGradleSync.orNull,
    apolloLinkSqlite = apolloExtension.linkSqlite.orNull,
    apolloServiceCount = apolloExtension.serviceCount,
)
