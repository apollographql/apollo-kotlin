package com.apollographql.apollo3.buildlogic.plugin

import com.apollographql.apollo3.buildlogic.configureJavaAndKotlinCompilers
import com.apollographql.apollo3.buildlogic.configureRepositories
import com.apollographql.apollo3.buildlogic.workaroundForIssueKT51970
import configureTesting
import org.gradle.api.Plugin
import org.gradle.api.Project

class JvmTestConventionPlugin : Plugin<Project> {
  override fun apply(project: Project) {
    with(project) {
      configureRepositories()

      pluginManager.apply {
        apply("org.jetbrains.kotlin.jvm")
        apply("com.apollographql.apollo3")
      }

      configureJavaAndKotlinCompilers()

      configureTesting()

      workaroundForIssueKT51970()
    }
  }
}
