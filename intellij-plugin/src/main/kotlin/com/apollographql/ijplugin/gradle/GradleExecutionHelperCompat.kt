package com.apollographql.ijplugin.gradle

import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskId
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskNotificationListener
import org.gradle.tooling.BuildLauncher
import org.gradle.tooling.ModelBuilder
import org.gradle.tooling.ProjectConnection
import org.jetbrains.plugins.gradle.service.execution.GradleExecutionHelper
import org.jetbrains.plugins.gradle.settings.GradleExecutionSettings

class GradleExecutionHelperCompat {
  private val gradleExecutionHelper = GradleExecutionHelper()

  fun <T> execute(
      projectPath: String,
      settings: GradleExecutionSettings?,
      f: (ProjectConnection) -> T,
  ): T {
    return gradleExecutionHelper.execute(projectPath, settings, f)
  }

  fun getBuildLauncher(
      connection: ProjectConnection,
      id: ExternalSystemTaskId,
      tasksAndArguments: List<String>,
      settings: GradleExecutionSettings,
      listener: ExternalSystemTaskNotificationListener,
  ): BuildLauncher {
    return gradleExecutionHelper.getBuildLauncher(connection, id, tasksAndArguments, settings, listener)
  }

  fun <T> getModelBuilder(
      modelType: Class<T>,
      connection: ProjectConnection,
      id: ExternalSystemTaskId,
      settings: GradleExecutionSettings,
      listener: ExternalSystemTaskNotificationListener,
  ): ModelBuilder<T> {
    val operation: ModelBuilder<T> = connection.model(modelType);
    GradleExecutionHelper.prepare(connection, operation, id, settings, listener)
    return operation
  }
}
