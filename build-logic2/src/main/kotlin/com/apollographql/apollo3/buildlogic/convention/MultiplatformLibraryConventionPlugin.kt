package com.apollographql.apollo3.buildlogic.convention

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.provider.Property
import org.gradle.api.tasks.bundling.Jar
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

class MultiplatformLibraryConventionPlugin : Plugin<Project> {
  override fun apply(project: Project) {
    with(project) {
      val extension: Extension = extensions.create("apolloConvention", Extension::class.java)

      pluginManager.apply {
        apply("org.jetbrains.kotlin.multiplatform")
      }

      tasks.withType(KotlinCompile::class.java) {
        kotlinOptions {
          allWarningsAsErrors = true
        }
      }

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
