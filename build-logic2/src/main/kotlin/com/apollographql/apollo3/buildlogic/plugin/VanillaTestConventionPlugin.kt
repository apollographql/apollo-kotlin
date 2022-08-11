package com.apollographql.apollo3.buildlogic.plugin

import com.apollographql.apollo3.buildlogic.configureJavaAndKotlinCompilers
import com.apollographql.apollo3.buildlogic.configureRepositories
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
