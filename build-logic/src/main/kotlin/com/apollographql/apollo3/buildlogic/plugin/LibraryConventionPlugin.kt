package com.apollographql.apollo3.buildlogic.plugin

import configureJavaAndKotlinCompilers
import configureMppDefaults
import configurePublishing
import configureRepositories
import configureTesting
import org.gradle.api.Action
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.bundling.Jar

class LibraryConventionPlugin : Plugin<Project> {
  override fun apply(project: Project) {
    with(project) {
      group = property("GROUP")!!
      version = property("VERSION_NAME")!!

      configureRepositories()

      val extension = extensions.create("apolloLibrary", Extension::class.java)

      pluginManager.apply {
        apply("org.jetbrains.kotlin.multiplatform")
      }

      configureJavaAndKotlinCompilers(treatWarningsAsErrors = true)

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

  abstract class Extension(private val project: Project) {
    interface MppConfiguration {
      val withJs: Property<Boolean>
      val withLinux: Property<Boolean>
    }

    abstract val javaModuleName: Property<String>

    @get:Nested
    abstract val mppConfiguration: MppConfiguration

    fun mpp(action: Action<MppConfiguration>) {
      action.execute(mppConfiguration)
      project.configureMppDefaults(
          withJs = mppConfiguration.withJs.getOrElse(true),
          withLinux = mppConfiguration.withLinux.getOrElse(true)
      )
    }
  }
}
