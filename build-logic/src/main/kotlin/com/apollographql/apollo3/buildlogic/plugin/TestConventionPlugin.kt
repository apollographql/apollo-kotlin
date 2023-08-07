package com.apollographql.apollo3.buildlogic.plugin

import commonSetup
import configureJavaAndKotlinCompilers
import configureMppTestsDefaults
import configureTesting
import hostTarget
import optIn
import org.gradle.api.Action
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.Nested

class TestConventionPlugin : Plugin<Project> {
  override fun apply(project: Project) {
    with(project) {
      extensions.create("apolloTest", Extension::class.java)

      commonSetup()
      configureJavaAndKotlinCompilers()
      optIn(
          "com.apollographql.apollo3.annotations.ApolloExperimental",
          "com.apollographql.apollo3.annotations.ApolloInternal", // for runTest
      )

      configureTesting()
    }
  }

  abstract class Extension(private val project: Project) {
    abstract class MppConfiguration {
      abstract val withJs: Property<Boolean>
      abstract val withJvm: Property<Boolean>
      abstract val browserTest: Property<Boolean>
      abstract val appleTargets: SetProperty<String>

      init {
        appleTargets.convention(null as List<String>?)
      }
    }

    @get:Nested
    abstract val mppConfiguration: MppConfiguration

    fun mpp(action: Action<MppConfiguration>) {
      action.execute(mppConfiguration)
      project.configureMppTestsDefaults(
          withJs = mppConfiguration.withJs.getOrElse(true),
          withJvm = mppConfiguration.withJvm.getOrElse(true),
          browserTest = mppConfiguration.browserTest.getOrElse(false),
          appleTargets = mppConfiguration.appleTargets.getOrElse(setOf(hostTarget))
      )
    }
  }
}
