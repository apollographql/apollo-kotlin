package com.apollographql.ijplugin.gradle

import com.apollographql.ijplugin.util.logw
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.Project
import org.gradle.tooling.BuildLauncher
import org.gradle.tooling.GradleConnector
import org.gradle.tooling.ModelBuilder
import org.gradle.tooling.model.GradleProject
import org.jetbrains.plugins.gradle.settings.GradleExecutionSettings
import org.jetbrains.plugins.gradle.util.GradleConstants
import java.io.File

const val CODEGEN_GRADLE_TASK_NAME = "generateApolloSources"

fun Project.getGradleRootPath(): String? {
  val rootProjectPath = ModuleManager.getInstance(this).modules.firstNotNullOfOrNull {
    ExternalSystemApiUtil.getExternalRootProjectPath(it)
  }
  if (rootProjectPath == null) logw("Could not get Gradle root project path")
  return rootProjectPath
}

fun GradleProject.allChildrenRecursively(): List<GradleProject> {
  return listOf(this) + children.flatMap { it.allChildrenRecursively() }
}

fun runGradleBuild(
    project: Project,
    gradleProjectPath: String,
    configureBuildLauncher: (BuildLauncher) -> BuildLauncher,
) {
  val executionSettings = ExternalSystemApiUtil.getExecutionSettings<GradleExecutionSettings>(
      project,
      gradleProjectPath,
      GradleConstants.SYSTEM_ID
  )
  val connection = GradleConnector.newConnector()
      .forProjectDirectory(File(gradleProjectPath))
      .connect()
  val buildLauncher = configureBuildLauncher(
      connection.newBuild()
          .setJavaHome(executionSettings.javaHome?.let { File(it) })
  )
  try {
    buildLauncher.run()
  } finally {
    connection.close()
  }
}

inline fun <reified T> getGradleModel(
    project: Project,
    gradleProjectPath: String,
    configureModelBuilder: (ModelBuilder<T>) -> ModelBuilder<T>,
): T? {
  val executionSettings = ExternalSystemApiUtil.getExecutionSettings<GradleExecutionSettings>(
      project,
      gradleProjectPath,
      GradleConstants.SYSTEM_ID
  )
  val connection = GradleConnector.newConnector()
      .forProjectDirectory(File(gradleProjectPath))
      .connect()
  val modelBuilder = configureModelBuilder(
      connection.model(T::class.java)
          .setJavaHome(executionSettings.javaHome?.let { File(it) })
  )
  try {
    return modelBuilder.get()
  } finally {
    connection.close()
  }
}
