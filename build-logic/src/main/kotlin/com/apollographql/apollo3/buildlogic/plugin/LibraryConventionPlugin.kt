package com.apollographql.apollo3.buildlogic.plugin

import configureJavaAndKotlinCompilers
import configureMppDefaults
import configurePublishing
import configureTesting
import configureTests
import optIn
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

      extensions.create("apolloLibrary", Extension::class.java, project)

      configureJavaAndKotlinCompilers()
      optIn(
          "com.apollographql.apollo3.annotations.ApolloExperimental",
          "com.apollographql.apollo3.annotations.ApolloInternal"
      )

      configureTesting()

      configurePublishing()

      // Within the 'tests' project (a composite build), dependencies are automatically substituted to use the project's one.
      // But we don't want this, for example apollo-tooling depends on a published version of apollo-api.
      // So disable this behavior (see https://docs.gradle.org/current/userguide/composite_builds.html#deactivate_included_build_substitutions).
      configurations.all {
        resolutionStrategy.useGlobalDependencySubstitutionRules.set(false)
      }
    }
  }

  abstract class Extension(private val project: Project) {
    interface MppConfiguration {
      val withJs: Property<Boolean>
      val withLinux: Property<Boolean>
      val withAndroid: Property<Boolean>
    }

    @get:Nested
    abstract val mppConfiguration: MppConfiguration

    fun mpp(action: Action<MppConfiguration>) {
      action.execute(mppConfiguration)
      project.configureMppDefaults(
          withJs = mppConfiguration.withJs.getOrElse(true),
          withLinux = mppConfiguration.withLinux.getOrElse(true),
          withAndroid = mppConfiguration.withAndroid.getOrElse(false)
      )
    }

    fun javaModuleName(javaModuleName: String) {
      project.tasks.withType(Jar::class.java).configureEach {
        manifest {
          attributes(mapOf("Automatic-Module-Name" to javaModuleName))
        }
      }
    }

    fun runTestsWithJavaVersion(javaVersion: Int) {
      project.configureTests(javaVersion)
    }
  }
}
