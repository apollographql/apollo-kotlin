package com.apollographql.apollo3.buildlogic.plugin

import com.apollographql.apollo3.buildlogic.configureJavaAndKotlinCompilers
import com.apollographql.apollo3.buildlogic.configureMppDefaults
import com.apollographql.apollo3.buildlogic.treatWarningsAsErrors
import configureTesting
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.provider.Property
import org.gradle.api.tasks.bundling.Jar
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

class MultiplatformLibraryConventionPlugin : Plugin<Project> {
  override fun apply(project: Project) {
    with(project) {
      val extension = extensions.create("apolloConvention", Extension::class.java)

      pluginManager.apply {
        apply("org.jetbrains.kotlin.multiplatform")
      }

      configureJavaAndKotlinCompilers()

      treatWarningsAsErrors()

      configureTesting()

      tasks.withType(Jar::class.java).configureEach {
        extension.javaModuleName.orNull?.let { javaModuleName ->
          manifest {
            attributes(mapOf("Automatic-Module-Name" to javaModuleName))
          }
        }
      }
    }
  }

  abstract class Extension(private val project: Project) {
    abstract val javaModuleName: Property<String>

    fun kotlin(withJs: Boolean = true, withLinux: Boolean = true, configure: KotlinMultiplatformExtension.() -> Unit) {
      project.configureMppDefaults(withJs, withLinux)

      val kotlinExtension = project.extensions.findByName("kotlin") as KotlinMultiplatformExtension
      kotlinExtension.configure()
    }
  }
}
