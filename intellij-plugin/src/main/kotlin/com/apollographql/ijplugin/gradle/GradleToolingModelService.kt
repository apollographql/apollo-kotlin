package com.apollographql.ijplugin.gradle

import com.apollographql.apollo3.gradle.api.ApolloGradleToolingModel
import com.apollographql.ijplugin.graphql.GraphQLProjectFiles
import com.apollographql.ijplugin.graphql.GraphQLProjectFilesListener
import com.apollographql.ijplugin.project.apolloProjectService
import com.apollographql.ijplugin.util.logd
import com.apollographql.ijplugin.util.logw
import com.intellij.openapi.Disposable
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskId
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskNotificationListenerAdapter
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskType
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
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
import java.util.concurrent.Executors

class GradleToolingModelService(
    private val project: Project,
) : Disposable {
  private var gradleCancellation: CancellationTokenSource? = null
  private var abortRequested: Boolean = false
  private val gradleExecutorService = Executors.newSingleThreadExecutor()
  var graphQLProjectFiles: List<GraphQLProjectFiles> = emptyList()

  init {
    logd("project=${project.name}")
    startObserveGradleHasSynced()
  }

  private fun startObserveGradleHasSynced() {
    logd()
    project.messageBus.connect(this).subscribe(GradleHasSyncedListener.TOPIC, object : GradleHasSyncedListener {
      override fun gradleHasSynced() {
        logd()
        abortFetchToolingModels()

        if (project.apolloProjectService.isApolloKotlin3Project) {
          fetchToolingModels()
        }
      }
    })
  }

  private fun fetchToolingModels() {
    logd()

    if (gradleCancellation != null) {
      logd("Already running")
      return
    }

    val rootProjectPath = project.getGradleRootPath() ?: return
    gradleExecutorService.submit {
      abortRequested = false
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
      } ?: return@submit

      // We're only interested in projects that apply the Apollo plugin - and thus have the codegen task registered
      val allApolloGradleProjects: List<GradleProject> = (rootGradleProject.children + rootGradleProject)
          .filter { it.tasks.any { it.name == CODEGEN_GRADLE_TASK_NAME } }
      logd("allApolloGradleProjects=${allApolloGradleProjects.map { it.name }}")
      val allToolingModels = allApolloGradleProjects.mapNotNull { gradleProject ->
        if (abortRequested) {
          logd("Aborted")
          return@submit
        }
        gradleExecutionHelper.execute(gradleProject.projectDirectory.canonicalPath, executionSettings) { connection ->
          gradleCancellation = GradleConnector.newCancellationTokenSource()
          logd("Fetch tooling model for :${gradleProject.name}")
          return@execute try {
            val id = ExternalSystemTaskId.create(GRADLE_SYSTEM_ID, ExternalSystemTaskType.RESOLVE_PROJECT, project)
            gradleExecutionHelper.getModelBuilder(ApolloGradleToolingModel::class.java, id, executionSettings, connection, ExternalSystemTaskNotificationListenerAdapter.NULL_OBJECT)
                .withCancellationToken(gradleCancellation!!.token())
                .get()
          } catch (t: Throwable) {
            logw(t, "Couldn't fetch tooling model for :${gradleProject.name}")
            null
          } finally {
            gradleCancellation = null
          }
        }
      }

      logd("allToolingModels=$allToolingModels")
      computeGraphQLProjectFiles(allToolingModels)
    }
  }

  private fun computeGraphQLProjectFiles(toolingModels: List<ApolloGradleToolingModel>) {
    // Compute the GraphQLProjectFiles, taking into account the dependencies between projects
    val projectServiceToGraphQLProjectFiles = mutableMapOf<String, GraphQLProjectFiles>()
    fun getGraphQLProjectFiles(projectName: String, serviceName: String): GraphQLProjectFiles {
      val key = "$projectName/$serviceName"
      return projectServiceToGraphQLProjectFiles.getOrPut(key) {
        val toolingModel = toolingModels.first { it.projectName == projectName }
        val serviceInfo = toolingModel.serviceInfos.first { it.name == serviceName }
        val dependenciesProjectFiles = toolingModel.upstreamProjects.map { getGraphQLProjectFiles(it, serviceName) }
        GraphQLProjectFiles(
            name = key,
            schemaPaths = (serviceInfo.schemaFiles.mapNotNull { it.toProjectLocalPathOrNull() } +
                dependenciesProjectFiles.flatMap { it.schemaPaths })
                .distinct(),
            operationPaths = (serviceInfo.graphqlSrcDirs.mapNotNull { it.toProjectLocalPathOrNull() } +
                dependenciesProjectFiles.flatMap { it.operationPaths })
                .distinct(),
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
    abortRequested = true
    gradleCancellation?.cancel()
    gradleCancellation = null
  }

  override fun dispose() {
    logd("project=${project.name}")
    abortFetchToolingModels()
    gradleExecutorService.shutdown()
  }
}
