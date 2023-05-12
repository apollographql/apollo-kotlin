package com.apollographql.ijplugin.gradle

import com.apollographql.apollo3.gradle.api.ApolloGradleToolingModel
import com.apollographql.ijplugin.ApolloBundle
import com.apollographql.ijplugin.graphql.GraphQLProjectFiles
import com.apollographql.ijplugin.graphql.GraphQLProjectFilesListener
import com.apollographql.ijplugin.project.ApolloProjectListener
import com.apollographql.ijplugin.project.apolloProjectService
import com.apollographql.ijplugin.settings.SettingsListener
import com.apollographql.ijplugin.settings.SettingsState
import com.apollographql.ijplugin.settings.settingsState
import com.apollographql.ijplugin.util.dispose
import com.apollographql.ijplugin.util.isNotDisposed
import com.apollographql.ijplugin.util.logd
import com.apollographql.ijplugin.util.logw
import com.apollographql.ijplugin.util.newDisposable
import com.intellij.openapi.Disposable
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskId
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskNotificationListenerAdapter
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskType
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.util.CheckedDisposable
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFileManager
import org.gradle.tooling.CancellationTokenSource
import org.gradle.tooling.GradleConnector
import org.gradle.tooling.model.GradleProject
import org.jetbrains.kotlin.idea.configuration.GRADLE_SYSTEM_ID
import org.jetbrains.plugins.gradle.service.execution.GradleExecutionHelper
import org.jetbrains.plugins.gradle.settings.GradleExecutionSettings
import org.jetbrains.plugins.gradle.util.GradleConstants
import java.io.File

class GradleToolingModelService(
    private val project: Project,
) : Disposable {
  private var gradleHasSyncedDisposable: CheckedDisposable? = null

  private var fetchToolingModelsTask: FetchToolingModelsTask? = null

  var graphQLProjectFiles: List<GraphQLProjectFiles> = emptyList()

  init {
    logd("project=${project.name}")
    startObserveApolloProject()
    startOrStopObserveGradleHasSynced()
    startOrAbortFetchToolingModels()
    startObservingSettings()
  }

  private fun startObserveApolloProject() {
    logd()
    project.messageBus.connect(this).subscribe(ApolloProjectListener.TOPIC, object : ApolloProjectListener {
      override fun apolloProjectChanged(isApolloAndroid2Project: Boolean, isApolloKotlin3Project: Boolean) {
        logd("isApolloAndroid2Project=$isApolloAndroid2Project isApolloKotlin3Project=$isApolloKotlin3Project")
        startOrStopObserveGradleHasSynced()
        startOrAbortFetchToolingModels()
      }
    })
  }

  private fun shouldFetchToolingModels() = project.apolloProjectService.isApolloKotlin3Project &&
      project.settingsState.contributeConfigurationToGraphqlPlugin

  private fun startOrStopObserveGradleHasSynced() {
    logd()
    if (shouldFetchToolingModels()) {
      startObserveGradleHasSynced()
    } else {
      stopObserveGradleHasSynced()
    }
  }

  private fun startObserveGradleHasSynced() {
    logd()
    if (gradleHasSyncedDisposable.isNotDisposed()) {
      logd("Already observing")
      return
    }
    val disposable = newDisposable()
    gradleHasSyncedDisposable = disposable
    project.messageBus.connect(disposable).subscribe(GradleHasSyncedListener.TOPIC, object : GradleHasSyncedListener {
      override fun gradleHasSynced() {
        logd()
        startOrAbortFetchToolingModels()
      }
    })
  }

  private fun stopObserveGradleHasSynced() {
    logd()
    dispose(gradleHasSyncedDisposable)
    gradleHasSyncedDisposable = null
  }

  private fun startObservingSettings() {
    logd()
    project.messageBus.connect(this).subscribe(SettingsListener.TOPIC, object : SettingsListener {
      override fun settingsChanged(settingsState: SettingsState) {
        logd("settingsState=$settingsState")
        startOrAbortFetchToolingModels()
      }
    })
  }

  private fun startOrAbortFetchToolingModels() {
    logd()
    abortFetchToolingModels()
    if (shouldFetchToolingModels()) {
      fetchToolingModels()
    }
  }

  private fun fetchToolingModels() {
    logd()

    if (fetchToolingModelsTask?.gradleCancellation != null) {
      logd("Already running")
      return
    }

    fetchToolingModelsTask = FetchToolingModelsTask().apply { queue() }
  }

  private inner class FetchToolingModelsTask : Task.Backgroundable(
      project,
      @Suppress("DialogTitleCapitalization")
      ApolloBundle.message("GradleToolingModelService.fetchToolingModels.progress"),
      true,
  ) {
    var abortRequested: Boolean = false
    var gradleCancellation: CancellationTokenSource? = null

    override fun run(indicator: ProgressIndicator) {
      val rootProjectPath = project.getGradleRootPath() ?: return
      val gradleExecutionHelper = GradleExecutionHelper()
      val executionSettings = ExternalSystemApiUtil.getExecutionSettings<GradleExecutionSettings>(project, rootProjectPath, GradleConstants.SYSTEM_ID)
      val rootGradleProject = gradleExecutionHelper.execute(rootProjectPath, executionSettings) { connection ->
        gradleCancellation = GradleConnector.newCancellationTokenSource()
        logd("Fetch Gradle project model")
        return@execute try {
          val id = ExternalSystemTaskId.create(GRADLE_SYSTEM_ID, ExternalSystemTaskType.RESOLVE_PROJECT, project)
          gradleExecutionHelper.getModelBuilder(GradleProject::class.java, id, executionSettings, connection, ExternalSystemTaskNotificationListenerAdapter.NULL_OBJECT)
              .withCancellationToken(gradleCancellation!!.token())
              .get()
        } catch (t: Throwable) {
          logw(t, "Couldn't fetch Gradle project model")
          null
        } finally {
          gradleCancellation = null
        }
      } ?: return

      // We're only interested in projects that apply the Apollo plugin - and thus have the codegen task registered
      val allApolloGradleProjects: List<GradleProject> = (rootGradleProject.children + rootGradleProject)
          .filter { gradleProject -> gradleProject.tasks.any { task -> task.name == CODEGEN_GRADLE_TASK_NAME } }
      logd("allApolloGradleProjects=${allApolloGradleProjects.map { it.name }}")
      indicator.isIndeterminate = false
      val allToolingModels = allApolloGradleProjects.mapIndexedNotNull { index, gradleProject ->
        if (isAbortRequested())          return@run
        indicator.fraction = (index + 1).toDouble() / allApolloGradleProjects.size
        gradleExecutionHelper.execute(gradleProject.projectDirectory.canonicalPath, executionSettings) { connection ->
          gradleCancellation = GradleConnector.newCancellationTokenSource()
          logd("Fetch tooling model for :${gradleProject.name}")
          return@execute try {
            val id = ExternalSystemTaskId.create(GRADLE_SYSTEM_ID, ExternalSystemTaskType.RESOLVE_PROJECT, project)
            gradleExecutionHelper.getModelBuilder(ApolloGradleToolingModel::class.java, id, executionSettings, connection, ExternalSystemTaskNotificationListenerAdapter.NULL_OBJECT)
                .withCancellationToken(gradleCancellation!!.token())
                .get()
                .takeIf {
                  val isCompatibleVersion = it.versionMajor == ApolloGradleToolingModel.VERSION_MAJOR
                  if (!isCompatibleVersion) {
                    logw("Incompatible version of Apollo Gradle plugin in module :${gradleProject.name}: ${it.versionMajor} != ${ApolloGradleToolingModel.VERSION_MAJOR}, ignoring")
                  }
                  isCompatibleVersion
                }
          } catch (t: Throwable) {
            logw(t, "Couldn't fetch tooling model for :${gradleProject.name}")
            null
          } finally {
            gradleCancellation = null
          }
        }
      }

      logd("allToolingModels=$allToolingModels")
      if (isAbortRequested()) return
      computeGraphQLProjectFiles(allToolingModels)
    }

    private fun isAbortRequested(): Boolean {
      if (abortRequested) {
        logd("Aborted")
        return true
      }
      try {
        ProgressManager.checkCanceled()
      } catch (e: ProcessCanceledException) {
        logd("Canceled by user")
        return true
      }
      return false
    }

    override fun onCancel() {
      logd()
      abortFetchToolingModels()
    }

    override fun onFinished() {
      fetchToolingModelsTask = null
    }
  }

  private fun computeGraphQLProjectFiles(toolingModels: List<ApolloGradleToolingModel>) {
    // Compute the GraphQLProjectFiles, taking into account the dependencies between projects
    val allKnownProjectNames = toolingModels.map { it.projectName }
    val projectServiceToGraphQLProjectFiles = mutableMapOf<String, GraphQLProjectFiles>()
    fun getGraphQLProjectFiles(projectName: String, serviceName: String): GraphQLProjectFiles {
      val key = "$projectName/$serviceName"
      return projectServiceToGraphQLProjectFiles.getOrPut(key) {
        val toolingModel = toolingModels.first { it.projectName == projectName }
        val serviceInfo = toolingModel.serviceInfos.first { it.name == serviceName }
        val dependenciesProjectFiles = serviceInfo.upstreamProjects
            // The tooling model for some upstream projects might not have been fetched successfully - filter them out
            .filter { upstreamProject -> upstreamProject in allKnownProjectNames }
            .map { getGraphQLProjectFiles(it, serviceName) }
        GraphQLProjectFiles(
            name = key,
            schemaPaths = (serviceInfo.schemaFiles.mapNotNull { it.toProjectLocalPathOrNull() } +
                dependenciesProjectFiles.flatMap { it.schemaPaths })
                .distinct(),
            operationPaths = (serviceInfo.graphqlSrcDirs.mapNotNull { it.toProjectLocalPathOrNull() } +
                dependenciesProjectFiles.flatMap { it.operationPaths })
                .distinct(),
            endpointUrl = if (toolingModel.versionMinor >= ApolloGradleToolingModel.VERSION_MINOR) serviceInfo.endpointUrl else null,
            endpointHeaders = if (toolingModel.versionMinor >= ApolloGradleToolingModel.VERSION_MINOR) serviceInfo.endpointHeaders else null,
        )
      }
    }

    val graphQLProjectFiles = mutableListOf<GraphQLProjectFiles>()
    for (toolingModel in toolingModels) {
      for (serviceInfo in toolingModel.serviceInfos) {
        graphQLProjectFiles += getGraphQLProjectFiles(toolingModel.projectName, serviceInfo.name)
      }
    }
    this.graphQLProjectFiles = graphQLProjectFiles
    logd("graphQLProjectFiles=$graphQLProjectFiles")

    // Project files are available, notify interested parties
    project.messageBus.syncPublisher(GraphQLProjectFilesListener.TOPIC).projectFilesAvailable()
  }

  private fun File.toProjectLocalPathOrNull(): String? {
    val projectDir = project.guessProjectDir() ?: return null
    val virtualFile = VirtualFileManager.getInstance().findFileByNioPath(toPath()) ?: return null
    return VfsUtilCore.getRelativeLocation(virtualFile, projectDir)
  }

  private fun abortFetchToolingModels() {
    logd()
    fetchToolingModelsTask?.abortRequested = true
    fetchToolingModelsTask?.gradleCancellation?.cancel()
    fetchToolingModelsTask = null
  }

  override fun dispose() {
    logd("project=${project.name}")
    abortFetchToolingModels()
  }
}
