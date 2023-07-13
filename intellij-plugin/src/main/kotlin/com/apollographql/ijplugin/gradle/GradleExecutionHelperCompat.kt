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

  /**
   * < 232:
   * ```
   * public BuildLauncher getBuildLauncher(
   *     @NotNull final ExternalSystemTaskId id,
   *     @NotNull ProjectConnection connection,
   *     @Nullable GradleExecutionSettings settings,
   *     @NotNull ExternalSystemTaskNotificationListener listener
   * )
   * ```
   */
  private val getBuildLauncherMethodPre232 = GradleExecutionHelper::class.java.methods.firstOrNull {
    it.name == "getBuildLauncher" && it.parameterCount == 4
  }

  /**
   * ≥ 232:
   * ```
   * public @NotNull BuildLauncher getBuildLauncher(
   *     @NotNull ProjectConnection connection,
   *     @NotNull ExternalSystemTaskId id,
   *     @NotNull List<String> tasksAndArguments,
   *     @NotNull GradleExecutionSettings settings,
   *     @NotNull ExternalSystemTaskNotificationListener listener
   * )
   * ```
   */
  private val getBuildLauncherMethodPost232 = GradleExecutionHelper::class.java.methods.firstOrNull {
    it.name == "getBuildLauncher" && it.parameterCount == 5
  }

  /**
   * < 232:
   * ```
   * public <T> ModelBuilder<T> getModelBuilder(
   *     @NotNull Class<T> modelType,
   *     @NotNull final ExternalSystemTaskId id,
   *     @Nullable GradleExecutionSettings settings,
   *     @NotNull ProjectConnection connection,
   *     @NotNull ExternalSystemTaskNotificationListener listener
   * )
   */
  private val getModelBuilderMethodPre232 = GradleExecutionHelper::class.java.methods.firstOrNull {
    it.name == "getModelBuilder" && it.parameterTypes[1] == ExternalSystemTaskId::class.java
  }

  /**
   * ≥ 232:
   * ```
   * public <T> @NotNull ModelBuilder<T> getModelBuilder(
   *   @NotNull Class<T> modelType,
   *   @NotNull ProjectConnection connection,
   *   @NotNull ExternalSystemTaskId id,
   *   @NotNull GradleExecutionSettings settings,
   *   @NotNull ExternalSystemTaskNotificationListener listener
   * )
   */
  private val getModelBuilderMethodPost232 = GradleExecutionHelper::class.java.methods.firstOrNull {
    it.name == "getModelBuilder" && it.parameterTypes[1] == ProjectConnection::class.java
  }

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
    return if (getBuildLauncherMethodPre232 != null) {
      getBuildLauncherMethodPre232.invoke(gradleExecutionHelper, id, connection, settings, listener) as BuildLauncher
    } else {
      if (getBuildLauncherMethodPost232 == null) {
        error("Could not find GradleExecutionHelper.getBuildLauncher method for either < 232 or ≥ 232")
      }
      getBuildLauncherMethodPost232.invoke(gradleExecutionHelper, connection, id, tasksAndArguments, settings, listener) as BuildLauncher
    }
  }

  fun <T> getModelBuilder(
      modelType: Class<T>,
      connection: ProjectConnection,
      id: ExternalSystemTaskId,
      settings: GradleExecutionSettings,
      listener: ExternalSystemTaskNotificationListener,
  ): ModelBuilder<T> {
    @Suppress("UNCHECKED_CAST")
    return if (getModelBuilderMethodPre232 != null) {
      getModelBuilderMethodPre232.invoke(gradleExecutionHelper, modelType, id, settings, connection, listener) as ModelBuilder<T>
    } else {
      if (getModelBuilderMethodPost232 == null) {
        error("Could not find GradleExecutionHelper.getModelBuilder method for either < 232 or ≥ 232")
      }
      getModelBuilderMethodPost232.invoke(gradleExecutionHelper, modelType, connection, id, settings, listener) as ModelBuilder<T>
    }
  }
}
