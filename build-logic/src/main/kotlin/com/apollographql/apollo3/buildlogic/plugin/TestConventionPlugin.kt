package com.apollographql.apollo3.buildlogic.plugin

import configureJavaAndKotlinCompilers
import configureMppTestsDefaults
import configureTesting
import org.gradle.api.Action
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.Nested

class TestConventionPlugin : Plugin<Project> {
  override fun apply(project: Project) {
    with(project) {
      val extension = extensions.create("apolloTest", Extension::class.java)

      configureJavaAndKotlinCompilers(extension.allWarningsAsErrors)

      configureTesting()
    }
  }

  abstract class Extension(private val project: Project) {
    interface MppConfiguration {
      val withJs: Property<Boolean>
      val withJvm: Property<Boolean>
      val newMemoryManager: Property<Boolean>
      val appleTargets: SetProperty<String>
    }

    @get:Nested
    abstract val mppConfiguration: MppConfiguration
    abstract val allWarningsAsErrors: Property<Boolean>

    fun mpp(action: Action<MppConfiguration>) {
      action.execute(mppConfiguration)
      project.configureMppTestsDefaults(
          withJs = mppConfiguration.withJs.getOrElse(true),
          withJvm = mppConfiguration.withJvm.getOrElse(true),
          newMemoryManager = mppConfiguration.newMemoryManager.getOrElse(true),
          appleTargets = mppConfiguration.appleTargets.get().ifEmpty { setOf("macosArm64", "macosX64") }
      )
    }
  }
}
