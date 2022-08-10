package com.apollographql.apollo3.buildlogic.plugin

import com.apollographql.apollo3.buildlogic.configureJavaAndKotlinCompilers
import com.apollographql.apollo3.buildlogic.configurePublishing
import com.apollographql.apollo3.buildlogic.configureRepositories
import configureTesting
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.provider.Property
import org.gradle.api.tasks.bundling.Jar

class JvmLibraryConventionPlugin : Plugin<Project> {
  override fun apply(project: Project) {
    with(project) {
      group = property("GROUP")!!
      version = property("VERSION_NAME")!!

      configureRepositories()

      val extension = extensions.create("apolloConvention", Extension::class.java)

      pluginManager.apply {
        apply("org.jetbrains.kotlin.jvm")
      }

      configureJavaAndKotlinCompilers()

      configureTesting()

      configurePublishing()

      tasks.withType(Jar::class.java).configureEach {
        extension.javaModuleName.orNull?.let { javaModuleName ->
          manifest {
            attributes(mapOf("Automatic-Module-Name" to javaModuleName))
          }
        }
      }
    }
  }

  interface Extension {
    val javaModuleName: Property<String>
  }
}
