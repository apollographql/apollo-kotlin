package com.apollographql.apollo3.buildlogic.plugin

import configureJavaAndKotlinCompilers
import configureRepositories
import configureTesting
import org.gradle.api.Plugin
import org.gradle.api.Project

class VanillaTestConventionPlugin : Plugin<Project> {
  override fun apply(project: Project) {
    with(project) {
      configureRepositories()

      configureJavaAndKotlinCompilers()

      configureTesting()
    }
  }
}
