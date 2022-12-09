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
import workaroundForIssueKT51970

class TestConventionPlugin : Plugin<Project> {
  override fun apply(project: Project) {
    with(project) {
      extensions.create("apolloTest", Extension::class.java)

      configureJavaAndKotlinCompilers()

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

    fun mpp(action: Action<MppConfiguration>) {
      action.execute(mppConfiguration)
      project.configureMppTestsDefaults(
          withJs = mppConfiguration.withJs.getOrElse(true),
          withJvm = mppConfiguration.withJvm.getOrElse(true),
          newMemoryManager = mppConfiguration.newMemoryManager.getOrElse(true),
          appleTargets = mppConfiguration.appleTargets.get().ifEmpty { setOf("macosArm64", "macosX64") }
      )

      project.workaroundForIssueKT51970()
    }
  }
}
