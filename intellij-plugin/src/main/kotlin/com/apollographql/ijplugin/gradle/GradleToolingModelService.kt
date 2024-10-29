package com.apollographql.ijplugin.gradle

import com.apollographql.apollo.gradle.api.ApolloGradleToolingModel
import com.apollographql.ijplugin.project.ApolloProjectListener
import com.apollographql.ijplugin.project.ApolloProjectService
import com.apollographql.ijplugin.project.apolloProjectService
import com.apollographql.ijplugin.settings.ProjectSettingsListener
import com.apollographql.ijplugin.settings.ProjectSettingsState
import com.apollographql.ijplugin.settings.projectSettingsState
import com.apollographql.ijplugin.telemetry.telemetryService
import com.apollographql.ijplugin.util.dispose
import com.apollographql.ijplugin.util.isNotDisposed
import com.apollographql.ijplugin.util.logd
import com.apollographql.ijplugin.util.logw
import com.apollographql.ijplugin.util.newDisposable
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskId
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskNotificationListener
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskType
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.util.CheckedDisposable
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFileManager
import org.gradle.tooling.CancellationTokenSource
import org.gradle.tooling.GradleConnector
import org.gradle.tooling.model.GradleProject
import org.jetbrains.kotlin.idea.configuration.GRADLE_SYSTEM_ID
import org.jetbrains.plugins.gradle.settings.GradleExecutionSettings
import org.jetbrains.plugins.gradle.util.GradleConstants
import java.io.File

@Service(Service.Level.PROJECT)
class GradleToolingModelService(
    private val project: Project,
) : Disposable {
  private var gradleHasSyncedDisposable: CheckedDisposable? = null

  private var fetchToolingModelsTask: FetchToolingModelsTask? = null

  var apolloKotlinServices: List<ApolloKotlinService> = project.projectSettingsState.apolloKotlinServices
    private set

  init {
    logd("project=${project.name}")
    startObserveApolloProject()
    startOrStopObserveGradleHasSynced()
    startOrAbortFetchToolingModels()
    startObserveSettings()

    if (shouldFetchToolingModels()) {
      // Contribute immediately, even though the ApolloKotlinServices are not available yet. They will be contributed later when available.
      // This avoids falling back to the default schema discovery of the GraphQL plugin which can be problematic (see https://github.com/apollographql/apollo-kotlin/issues/6219)
      project.messageBus.syncPublisher(ApolloKotlinServiceListener.TOPIC).apolloKotlinServicesAvailable()
    }
  }

  private fun startObserveApolloProject() {
    logd()
    project.messageBus.connect(this).subscribe(ApolloProjectListener.TOPIC, object : ApolloProjectListener {
      override fun apolloProjectChanged(apolloVersion: ApolloProjectService.ApolloVersion) {
        logd("apolloVersion=$apolloVersion")
        startOrStopObserveGradleHasSynced()
        startOrAbortFetchToolingModels()
      }
    })
  }

  private fun shouldFetchToolingModels() = project.apolloProjectService.apolloVersion.isAtLeastV4 &&
      project.projectSettingsState.contributeConfigurationToGraphqlPlugin

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

  private fun startObserveSettings() {
    logd()
    project.messageBus.connect(this).subscribe(ProjectSettingsListener.TOPIC, object : ProjectSettingsListener {
      private var contributeConfigurationToGraphqlPlugin: Boolean = project.projectSettingsState.contributeConfigurationToGraphqlPlugin

      override fun settingsChanged(projectSettingsState: ProjectSettingsState) {
        val contributeConfigurationToGraphqlPluginChanged =
          contributeConfigurationToGraphqlPlugin != projectSettingsState.contributeConfigurationToGraphqlPlugin
        contributeConfigurationToGraphqlPlugin = projectSettingsState.contributeConfigurationToGraphqlPlugin
        logd("contributeConfigurationToGraphqlPluginChanged=$contributeConfigurationToGraphqlPluginChanged")
        if (contributeConfigurationToGraphqlPluginChanged) {
          startOrAbortFetchToolingModels()
        }
      }
    })
  }

  fun triggerFetchToolingModels() {
    logd()
    startOrAbortFetchToolingModels()
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

    fetchToolingModelsTask = FetchToolingModelsTask().also { ApplicationManager.getApplication().executeOnPooledThread(it) }
  }

  private inner class FetchToolingModelsTask : Runnable {
    var abortRequested: Boolean = false
    var gradleCancellation: CancellationTokenSource? = null

    override fun run() {
      try {
        doRun()
      } finally {
        fetchToolingModelsTask = null
      }
    }

    private fun doRun() {
      logd()
      val rootProjectPath = project.getGradleRootPath() ?: return
      val gradleExecutionHelper = GradleExecutionHelperCompat()
      val executionSettings =
        ExternalSystemApiUtil.getExecutionSettings<GradleExecutionSettings>(project, rootProjectPath, GradleConstants.SYSTEM_ID)
      val rootGradleProject = gradleExecutionHelper.execute(rootProjectPath, executionSettings) { connection ->
        gradleCancellation = GradleConnector.newCancellationTokenSource()
        logd("Fetch Gradle project model")
        return@execute try {
          val id = ExternalSystemTaskId.create(GRADLE_SYSTEM_ID, ExternalSystemTaskType.RESOLVE_PROJECT, project)
          gradleExecutionHelper.getModelBuilder(GradleProject::class.java, connection, id, executionSettings, ExternalSystemTaskNotificationListener.NULL_OBJECT)
              .withCancellationToken(gradleCancellation!!.token())
              .get()
        } catch (t: Throwable) {
          logw(t, "Couldn't fetch Gradle project model")
          null
        } finally {
          gradleCancellation = null
        }
      } ?: return
      project.telemetryService.gradleModuleCount = rootGradleProject.children.size + 1

      // We're only interested in projects that apply the Apollo plugin - and thus have the codegen task registered
      val allApolloGradleProjects: List<GradleProject> = rootGradleProject.allChildrenRecursively()
          .filter { gradleProject -> gradleProject.tasks.any { task -> task.name == CODEGEN_GRADLE_TASK_NAME } }
      logd("allApolloGradleProjects=${allApolloGradleProjects.map { it.path }}")
      project.telemetryService.apolloKotlinModuleCount = allApolloGradleProjects.size

      val allToolingModels = allApolloGradleProjects.mapIndexedNotNull { index, gradleProject ->
        if (isAbortRequested()) return@doRun
        gradleExecutionHelper.execute(gradleProject.projectDirectory.canonicalPath, executionSettings) { connection ->
          gradleCancellation = GradleConnector.newCancellationTokenSource()
          logd("Fetch tooling model for ${gradleProject.path}")
          return@execute try {
            val id = ExternalSystemTaskId.create(GRADLE_SYSTEM_ID, ExternalSystemTaskType.RESOLVE_PROJECT, project)
            gradleExecutionHelper.getModelBuilder(ApolloGradleToolingModel::class.java, connection, id, executionSettings, ExternalSystemTaskNotificationListener.NULL_OBJECT)
                .withCancellationToken(gradleCancellation!!.token())
                .get()
                .takeIf {
                  val isCompatibleVersion = it.versionMajor == ApolloGradleToolingModel.VERSION_MAJOR
                  if (!isCompatibleVersion) {
                    logw("Incompatible version of Apollo Gradle plugin in module ${gradleProject.path}: ${it.versionMajor} != ${ApolloGradleToolingModel.VERSION_MAJOR}, ignoring")
                  }
                  isCompatibleVersion
                }
          } catch (t: Throwable) {
            logw(t, "Couldn't fetch tooling model for ${gradleProject.path}")
            null
          } finally {
            gradleCancellation = null
          }
        }
      }

      logd("allToolingModels=$allToolingModels")
      if (isAbortRequested()) return
      computeApolloKotlinServices(allToolingModels)
      project.telemetryService.gradleToolingModels = allToolingModels.toSet()
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
  }

  private fun computeApolloKotlinServices(toolingModels: List<ApolloGradleToolingModel>) {
    // Compute the ApolloKotlinServices, taking into account the dependencies between projects
    val allKnownProjectPaths = toolingModels.map { it.projectPathCompat }
    val projectServiceToApolloKotlinServices = mutableMapOf<String, ApolloKotlinService>()

    fun getApolloKotlinService(projectPath: String, serviceName: String): ApolloKotlinService {
      val key = "$projectPath/$serviceName"
      return projectServiceToApolloKotlinServices.getOrPut(key) {
        val toolingModel = toolingModels.first { it.projectPathCompat == projectPath }
        val serviceInfo = toolingModel.serviceInfos.first { it.name == serviceName }
        val upstreamApolloKotlinServices = serviceInfo.upstreamProjectPathsCompat(toolingModel)
            // The tooling model for some upstream projects might not have been fetched successfully - filter them out
            .filter { upstreamProjectPath -> upstreamProjectPath in allKnownProjectPaths }
            .map { upstreamProjectPath -> getApolloKotlinService(upstreamProjectPath, serviceName) }
        ApolloKotlinService(
            gradleProjectPath = projectPath,
            serviceName = serviceName,
            schemaPaths = (serviceInfo.schemaFiles.mapNotNull { it.toProjectLocalPathOrNull() } +
                upstreamApolloKotlinServices.flatMap { it.schemaPaths })
                .distinct(),
            operationPaths = (serviceInfo.graphqlSrcDirs.mapNotNull { it.toProjectLocalPathOrNull() } +
                upstreamApolloKotlinServices.flatMap { it.operationPaths })
                .distinct(),
            endpointUrl = serviceInfo.endpointUrlCompat(toolingModel),
            endpointHeaders = serviceInfo.endpointHeadersCompat(toolingModel),
            upstreamServiceIds = upstreamApolloKotlinServices.map { it.id },
            useSemanticNaming = serviceInfo.useSemanticNamingCompat(toolingModel),
        )
      }
    }

    val apolloKotlinServices = mutableListOf<ApolloKotlinService>()
    for (toolingModel in toolingModels) {
      for (serviceInfo in toolingModel.serviceInfos) {
        apolloKotlinServices += getApolloKotlinService(toolingModel.projectPathCompat, serviceInfo.name)
      }
    }
    logd("apolloKotlinServices=$apolloKotlinServices")
    this.apolloKotlinServices = apolloKotlinServices
    // Cache the ApolloKotlinServices into the project settings
    project.projectSettingsState.apolloKotlinServices = apolloKotlinServices

    // Services are available, notify interested parties
    project.messageBus.syncPublisher(ApolloKotlinServiceListener.TOPIC).apolloKotlinServicesAvailable()
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

  companion object {
    fun getApolloKotlinServices(project: Project): List<ApolloKotlinService> {
      if (!project.apolloProjectService.isInitialized) return emptyList()
      return project.service<GradleToolingModelService>().apolloKotlinServices
    }
  }
}

private fun GradleProject.allChildrenRecursively(): List<GradleProject> {
  return listOf(this) + children.flatMap { it.allChildrenRecursively() }
}

private val ApolloGradleToolingModel.projectPathCompat: String
  get() = if (versionMinor >= 3) {
    projectPath
  } else {
    @Suppress("DEPRECATION")
    projectName
  }

private fun ApolloGradleToolingModel.ServiceInfo.upstreamProjectPathsCompat(toolingModel: ApolloGradleToolingModel) =
  if (toolingModel.versionMinor >= 3) {
    upstreamProjectPaths
  } else {
    @Suppress("DEPRECATION")
    upstreamProjects
  }

private fun ApolloGradleToolingModel.ServiceInfo.endpointUrlCompat(toolingModel: ApolloGradleToolingModel) =
  if (toolingModel.versionMinor >= 1) endpointUrl else null

private fun ApolloGradleToolingModel.ServiceInfo.endpointHeadersCompat(toolingModel: ApolloGradleToolingModel) =
  if (toolingModel.versionMinor >= 1) endpointHeaders else null

private fun ApolloGradleToolingModel.ServiceInfo.useSemanticNamingCompat(toolingModel: ApolloGradleToolingModel) =
  if (toolingModel.versionMinor >= 4) useSemanticNaming else true

val Project.gradleToolingModelService get() = service<GradleToolingModelService>()
