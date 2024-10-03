package com.apollographql.ijplugin.gradle

import com.apollographql.ijplugin.ApolloBundle
import com.apollographql.ijplugin.project.apolloProjectService
import com.apollographql.ijplugin.telemetry.TelemetryEvent
import com.apollographql.ijplugin.telemetry.telemetryService
import com.apollographql.ijplugin.util.logd
import com.apollographql.ijplugin.util.logw
import com.apollographql.ijplugin.util.showNotification
import com.intellij.ide.BrowserUtil
import com.intellij.notification.NotificationAction
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskId
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskNotificationListener
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskType
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import org.gradle.tooling.Failure
import org.gradle.tooling.model.GradleProject
import org.jetbrains.kotlin.idea.configuration.GRADLE_SYSTEM_ID
import org.jetbrains.plugins.gradle.settings.GradleExecutionSettings
import org.jetbrains.plugins.gradle.util.GradleConstants

class DownloadSchemaAction : AnAction() {
  override fun actionPerformed(e: AnActionEvent) {
    logd()
    val project = e.project ?: return
    project.telemetryService.logEvent(TelemetryEvent.ApolloIjDownloadSchema())
    DownloadSchemaTask(project).queue()
  }

  override fun update(e: AnActionEvent) {
    e.presentation.isEnabled = e.project?.apolloProjectService?.apolloVersion?.isAtLeastV3 == true && e.project?.getGradleRootPath() != null
  }

  override fun getActionUpdateThread() = ActionUpdateThread.BGT
}

private val DOWNLOAD_SCHEMA_TASK_REGEX = Regex("download.+ApolloSchemaFrom(Introspection|Registry)")
private const val SCHEMA_CONFIGURATION_DOC_URL = "https://www.apollographql.com/docs/kotlin/advanced/plugin-configuration#downloading-a-schema"

private class DownloadSchemaTask(project: Project) : Task.Backgroundable(
    project,
    ApolloBundle.message("action.DownloadSchemaAction.progress"),
    false,
) {
  override fun run(indicator: ProgressIndicator) {
    val rootProjectPath = project.getGradleRootPath() ?: return
    val gradleExecutionHelper = GradleExecutionHelperCompat()
    val executionSettings = ExternalSystemApiUtil.getExecutionSettings<GradleExecutionSettings>(project, rootProjectPath, GradleConstants.SYSTEM_ID)
    val rootGradleProject = gradleExecutionHelper.execute(rootProjectPath, executionSettings) { connection ->
      logd("Fetch Gradle project model")
      return@execute try {
        val id = ExternalSystemTaskId.create(GRADLE_SYSTEM_ID, ExternalSystemTaskType.RESOLVE_PROJECT, project)
        gradleExecutionHelper.getModelBuilder(GradleProject::class.java, connection, id, executionSettings, ExternalSystemTaskNotificationListener.NULL_OBJECT)
            .get()
      } catch (t: Throwable) {
        logw(t, "Couldn't fetch Gradle project model")
        null
      }
    } ?: return

    val allDownloadSchemaTasks: List<String> = (rootGradleProject.children + rootGradleProject)
        .flatMap { gradleProject -> gradleProject.tasks.filter { task -> task.name.matches(DOWNLOAD_SCHEMA_TASK_REGEX) } }
        .map { it.name }
        .distinct()
    logd("allDownloadSchemaTasks=$allDownloadSchemaTasks")
    if (allDownloadSchemaTasks.isEmpty()) {
      showNotification(
          project = project,
          title = ApolloBundle.message("action.DownloadSchemaAction.noTasksFound.title"),
          content = ApolloBundle.message("action.DownloadSchemaAction.noTasksFound.content"),
          type = NotificationType.WARNING,
          NotificationAction.create(ApolloBundle.message("action.DownloadSchemaAction.openDocumentation")) { _, _ ->
            BrowserUtil.browse(SCHEMA_CONFIGURATION_DOC_URL, project)
          }
      )
      return
    }

    gradleExecutionHelper.execute(rootProjectPath, executionSettings) { connection ->
      try {
        val id = ExternalSystemTaskId.create(GRADLE_SYSTEM_ID, ExternalSystemTaskType.EXECUTE_TASK, project)
        gradleExecutionHelper.getBuildLauncher(connection, id, allDownloadSchemaTasks, executionSettings, ExternalSystemTaskNotificationListener.NULL_OBJECT)
            .forTasks(*allDownloadSchemaTasks.toTypedArray())
            .addProgressListener(object : SimpleProgressListener() {
              override fun onFailure(failures: List<Failure>) {
                super.onFailure(failures)
                showNotification(
                    project = project,
                    title = ApolloBundle.message("action.DownloadSchemaAction.buildFail.title"),
                    content = ApolloBundle.message("action.DownloadSchemaAction.buildFail.content", failures.firstOrNull()?.message
                        ?: "(no message)", allDownloadSchemaTasks.joinToString(" ")),
                    type = NotificationType.WARNING
                )
              }

              override fun onSuccess() {
                super.onSuccess()
                val schemas = if (allDownloadSchemaTasks.size > 1) ApolloBundle.message("action.DownloadSchemaAction.schema.plural") else ApolloBundle.message("action.DownloadSchemaAction.schema.singular")
                showNotification(
                    project = project,
                    content = ApolloBundle.message("action.DownloadSchemaAction.buildSuccess.content", schemas),
                    type = NotificationType.INFORMATION
                )
              }
            })
            .run()
        logd("Gradle execution finished")
      } catch (t: Throwable) {
        logd(t, "Gradle execution failed")
      }
    }
  }
}
