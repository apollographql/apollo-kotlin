package com.apollographql.ijplugin.services.impl

import com.apollographql.ijplugin.services.ApolloProjectService
import com.apollographql.ijplugin.util.apolloGeneratedSourcesRoot
import com.apollographql.ijplugin.util.isApolloAndroid2Project
import com.apollographql.ijplugin.util.isApolloKotlin3Project
import com.apollographql.ijplugin.util.isNullOrDisposed
import com.apollographql.ijplugin.util.logd
import com.apollographql.ijplugin.util.logw
import com.apollographql.ijplugin.util.runWriteActionInEdt
import com.intellij.ProjectTopics
import com.intellij.lang.jsgraphql.GraphQLFileType
import com.intellij.openapi.Disposable
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskId
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskNotificationListenerAdapter
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskType
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileEditorManagerEvent
import com.intellij.openapi.fileEditor.FileEditorManagerListener
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ModuleRootEvent
import com.intellij.openapi.roots.ModuleRootListener
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.psi.PsiDocumentManager
import org.gradle.tooling.CancellationTokenSource
import org.gradle.tooling.GradleConnector
import org.gradle.tooling.events.FailureResult
import org.gradle.tooling.events.FinishEvent
import org.gradle.tooling.events.ProgressListener
import org.gradle.tooling.events.StartEvent
import org.gradle.tooling.events.SuccessResult
import org.jetbrains.kotlin.idea.framework.GRADLE_SYSTEM_ID
import org.jetbrains.plugins.gradle.service.execution.GradleExecutionHelper
import org.jetbrains.plugins.gradle.settings.GradleExecutionSettings
import org.jetbrains.plugins.gradle.util.GradleConstants
import java.util.concurrent.Executors

private const val CODEGEN_GRADLE_TASK_NAME = "generateApolloSources"

class ApolloProjectServiceImpl(
    private val project: Project,
) : ApolloProjectService, Disposable {

  override var isApolloAndroid2Project: Boolean = false
  override var isApolloKotlin3Project: Boolean = false

  private var documentChangesDisposable: Disposable? = null
  private var fileEditorChangesDisposable: Disposable? = null

  private var dirtyGqlDocument: Document? = null

  private var gradleCodegenCancellation: CancellationTokenSource? = null

  private val gradleExecutorService = Executors.newSingleThreadExecutor()

  init {
    logd("project=${project.name}")
    onLibrariesChanged()
    startObserveLibraries()
  }

  private fun startObserveLibraries() {
    logd()
    val messageBusConnection = project.messageBus.connect(this)
    messageBusConnection.subscribe(ProjectTopics.PROJECT_ROOTS, object : ModuleRootListener {
      override fun rootsChanged(event: ModuleRootEvent) {
        logd("event=$event")
        onLibrariesChanged()
      }
    })
  }

  private fun onLibrariesChanged() {
    logd()
    synchronized(this) {
      isApolloAndroid2Project = project.isApolloAndroid2Project()
      isApolloKotlin3Project = project.isApolloKotlin3Project()
      logd("isApolloAndroid2Project=$isApolloAndroid2Project isApolloKotlin3Project=$isApolloKotlin3Project")
      if (isApolloKotlin3Project) {
        // To make the codegen more reactive, any touched GraphQL document will automatically be saved (thus triggering Gradle)
        // as soon as the current editor is changed.
        startObserveDocumentChanges()
        startObserveFileEditorChanges()

        startContinuousGradleCodegen()
      } else {
        stopObserveDocumentChanges()
        stopObserveFileEditorChanges()
        stopContinuousGradleCodegen()
      }
    }
  }

  private fun startObserveDocumentChanges() {
    logd()

    if (!documentChangesDisposable.isNullOrDisposed()) {
      logd("Already observing")
      return
    }

    val disposable = Disposer.newDisposable(this, "documentChanges")
    documentChangesDisposable = disposable
    EditorFactory.getInstance().eventMulticaster.addDocumentListener(object : DocumentListener {
      override fun documentChanged(event: DocumentEvent) {
        val vFile = PsiDocumentManager.getInstance(project).getPsiFile(event.document)?.virtualFile ?: return
        val isGqlFileInProject = vFile.fileType is GraphQLFileType && ProjectRootManager.getInstance(project).fileIndex.getModuleForFile(vFile) != null
        if (!isGqlFileInProject) {
          // Not a GraphQL file or not from this project: ignore
          return
        }
        logd("${vFile.path} has changed")
        dirtyGqlDocument = event.document
      }
    }, disposable)
  }

  private fun stopObserveDocumentChanges() {
    logd()
    documentChangesDisposable?.dispose()
    documentChangesDisposable = null
  }

  private fun startObserveFileEditorChanges() {
    logd()

    if (!fileEditorChangesDisposable.isNullOrDisposed()) {
      logd("Already observing")
      return
    }

    val disposable = Disposer.newDisposable(this, "fileEditorChanges")
    fileEditorChangesDisposable = disposable
    project.messageBus.connect(disposable).subscribe(FileEditorManagerListener.FILE_EDITOR_MANAGER, object : FileEditorManagerListener {
      override fun selectionChanged(event: FileEditorManagerEvent) {
        logd(event.newFile)
        dirtyGqlDocument?.let {
          dirtyGqlDocument = null
          runWriteActionInEdt {
            try {
              val fileDocumentManager = FileDocumentManager.getInstance()
              logd("Saving document ${fileDocumentManager.getFile(it)}")
              fileDocumentManager.saveDocument(it)
            } catch (e: Exception) {
              logw(e, "Failed to save document")
            }
          }
        }
      }
    })
  }

  private fun stopObserveFileEditorChanges() {
    logd()
    fileEditorChangesDisposable?.dispose()
    fileEditorChangesDisposable = null
  }

  override fun notifyGradleHasSynced() {
    logd()
    if (isApolloKotlin3Project) {
      stopContinuousGradleCodegen()
      startContinuousGradleCodegen()
    }
  }

  private fun startContinuousGradleCodegen() {
    logd()

    if (gradleCodegenCancellation != null) {
      logd("Already running")
      return
    }

    val modules = ModuleManager.getInstance(project).modules
    val firstModule = modules.firstOrNull()
    val rootProjectPath = ExternalSystemApiUtil.getExternalRootProjectPath(firstModule)
    if (rootProjectPath == null) {
      logw("Could not get root project path for module ${firstModule?.name}")
      return
    }
    val executionSettings = ExternalSystemApiUtil.getExecutionSettings<GradleExecutionSettings>(project, rootProjectPath, GradleConstants.SYSTEM_ID)

    gradleExecutorService.submit {
      val gradleExecutionHelper = GradleExecutionHelper()
      gradleExecutionHelper.execute(rootProjectPath, executionSettings) { connection ->
        gradleCodegenCancellation = GradleConnector.newCancellationTokenSource()
        logd("Start Gradle")
        try {
          val id = ExternalSystemTaskId.create(GRADLE_SYSTEM_ID, ExternalSystemTaskType.REFRESH_TASKS_LIST, project)
          gradleExecutionHelper.getBuildLauncher(id, connection, executionSettings, ExternalSystemTaskNotificationListenerAdapter.NULL_OBJECT)
              .forTasks(CODEGEN_GRADLE_TASK_NAME)
              .withCancellationToken(gradleCodegenCancellation!!.token())
              .addArguments("--continuous")
              .addProgressListener(ProgressListener { event ->
                when {
                  event is StartEvent && event.descriptor.name == "Run build" -> {
                    logd("Gradle build started")
                  }

                  event is FinishEvent && event.descriptor.name == "Run build" -> {
                    when (val result = event.result) {
                      is FailureResult -> {
                        logd("Gradle build failed: ${result.failures.map { it.message }}")
                      }

                      is SuccessResult -> {
                        logd("Gradle build success, marking generated source roots as dirty")
                        // Mark the generated sources dirty so the files are visible to the IDE
                        val generatedSourceRoots = modules.mapNotNull { it.apolloGeneratedSourcesRoot() }
                        logd("Mark dirty $generatedSourceRoots")
                        VfsUtil.markDirtyAndRefresh(true, true, true, *generatedSourceRoots.toTypedArray())
                      }
                    }
                  }
                }
              })
              .run()
          logd("Gradle execution finished")
        } catch (t: Throwable) {
          logd(t, "Gradle execution failed")
        } finally {
          gradleCodegenCancellation = null
        }
      }
    }
  }

  private fun stopContinuousGradleCodegen() {
    logd()
    gradleCodegenCancellation?.cancel()
    gradleCodegenCancellation = null
  }

  override fun dispose() {
    logd("project=${project.name}")
    stopContinuousGradleCodegen()
  }
}
