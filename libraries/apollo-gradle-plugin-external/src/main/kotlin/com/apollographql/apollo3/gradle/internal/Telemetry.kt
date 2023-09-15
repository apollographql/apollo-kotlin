package com.apollographql.apollo3.gradle.internal

import com.apollographql.apollo3.gradle.api.androidExtension
import org.gradle.api.Project

internal fun getTelemetryData(project: Project) = DefaultTelemetryData(
    gradleVersion = project.gradle.gradleVersion,
    androidMinSdk = project.androidExtension?.minSdk,
    androidTargetSdk = project.androidExtension?.targetSdk,
)
