package com.apollographql.apollo3.buildlogic.plugin

import configureJavaAndKotlinCompilers
import configureRepositories
import configureTesting
import org.gradle.api.Plugin
import org.gradle.api.Project

class AndroidTestConventionPlugin : Plugin<Project> {
  override fun apply(project: Project) {
    with(project) {
      configureRepositories()

      pluginManager.apply {
        apply("com.android.library")
        apply("org.jetbrains.kotlin.android")
        apply("com.apollographql.apollo3")
      }

      configureJavaAndKotlinCompilers()

      configureTesting()
    }
  }
}
