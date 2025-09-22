package com.apollographql.apollo.gradle.internal

import org.gradle.api.Project

internal fun getTelemetryData(project: Project, apolloExtension: DefaultApolloExtension) = DefaultTelemetryData(
    gradleVersion = project.gradle.gradleVersion,
    androidMinSdk = apolloExtension.agpOrNull?.minSdk(),
    androidTargetSdk = apolloExtension.agpOrNull?.targetSdk(),
    androidCompileSdk = apolloExtension.agpOrNull?.compileSdk(),
    androidAgpVersion = apolloExtension.agpOrNull?.version,
    apolloServiceTelemetryData = apolloExtension.getServiceTelemetryData(),
    apolloGenerateSourcesDuringGradleSync = apolloExtension.generateSourcesDuringGradleSync.orNull,
    apolloLinkSqlite = apolloExtension.linkSqlite.orNull,
    apolloServiceCount = apolloExtension.serviceCount,
)
