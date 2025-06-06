package com.apollographql.ijplugin.codegen

import com.apollographql.ijplugin.gradle.CODEGEN_GRADLE_TASK_NAME
import com.apollographql.ijplugin.gradle.GradleHasSyncedListener
import com.apollographql.ijplugin.gradle.SimpleProgressListener
import com.apollographql.ijplugin.gradle.getGradleRootPath
import com.apollographql.ijplugin.gradle.runGradleBuild
import com.apollographql.ijplugin.project.ApolloProjectListener
import com.apollographql.ijplugin.project.ApolloProjectService
import com.apollographql.ijplugin.project.apolloProjectService
import com.apollographql.ijplugin.settings.ProjectSettingsListener
import com.apollographql.ijplugin.settings.ProjectSettingsState
import com.apollographql.ijplugin.settings.projectSettingsState
import com.apollographql.ijplugin.util.apolloGeneratedSourcesRoots
import com.apollographql.ijplugin.util.dispose
import com.apollographql.ijplugin.util.isNotDisposed
import com.apollographql.ijplugin.util.logd
import com.apollographql.ijplugin.util.logw
import com.apollographql.ijplugin.util.newDisposable
import com.apollographql.ijplugin.util.runWriteActionInEdt
import com.intellij.lang.jsgraphql.GraphQLFileType
import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileEditorManagerEvent
import com.intellij.openapi.fileEditor.FileEditorManagerListener
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.util.CheckedDisposable
import com.intellij.openapi.vfs.VfsUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.gradle.tooling.CancellationTokenSource
import org.gradle.tooling.GradleConnector

@Service(Service.Level.PROJECT)
class ApolloCodegenService(
    private val project: Project,
    private val coroutineScope: CoroutineScope,
) : Disposable {
  private var documentChangesDisposable: CheckedDisposable? = null
  private var fileEditorChangesDisposable: CheckedDisposable? = null
  private var gradleHasSyncedDisposable: CheckedDisposable? = null

  private var dirtyGqlDocument: Document? = null

  private var gradleCodegenCancellation: CancellationTokenSource? = null

  init {
    logd("project=${project.name}")
    startOrStopCodegenObservers()
    startObserveApolloProject()
    startObserveSettings()
  }

  private fun startObserveApolloProject() {
    logd()
    project.messageBus.connect(this).subscribe(ApolloProjectListener.TOPIC, object : ApolloProjectListener {
      override fun apolloProjectChanged(apolloVersion: ApolloProjectService.ApolloVersion) {
        logd("apolloVersion=$apolloVersion")
        startOrStopCodegenObservers()
      }
    })
  }

  private fun startObserveSettings() {
    logd()
    project.messageBus.connect(this).subscribe(ProjectSettingsListener.TOPIC, object : ProjectSettingsListener {
      override fun settingsChanged(projectSettingsState: ProjectSettingsState) {
        logd("projectSettingsState=$projectSettingsState")
        startOrStopCodegenObservers()
      }
    })
  }

  private fun shouldTriggerCodegenAutomatically() =
    project.apolloProjectService.apolloVersion.isAtLeastV3 && project.projectSettingsState.automaticCodegenTriggering

  private fun startOrStopCodegenObservers() {
    if (shouldTriggerCodegenAutomatically()) {
      // To make the codegen more reactive, any touched GraphQL document will automatically be saved (thus triggering Gradle)
      // as soon as the current editor is changed.
      startObserveDocumentChanges()
      startObserveFileEditorChanges()

      startContinuousGradleCodegen()

      // Since we rely on Gradle's continuous build, which is not re-triggered when Gradle build files change, observe that
      // ourselves and restart the build when it happens.
      startObserveGradleHasSynced()
    } else {
      stopObserveDocumentChanges()
      stopObserveFileEditorChanges()
      stopContinuousGradleCodegen()
      stopObserveGradleHasSynced()
    }
  }

  private fun startObserveDocumentChanges() {
    logd()

    if (documentChangesDisposable.isNotDisposed()) {
      logd("Already observing")
      return
    }

    val disposable = newDisposable()
    documentChangesDisposable = disposable
    EditorFactory.getInstance().eventMulticaster.addDocumentListener(object : DocumentListener {
      override fun documentChanged(event: DocumentEvent) {
        val vFile = FileDocumentManager.getInstance().getFile(event.document) ?: return
        val isGqlFileInProject =
          vFile.fileType is GraphQLFileType && ProjectRootManager.getInstance(project).fileIndex.getModuleForFile(vFile) != null
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
    dispose(documentChangesDisposable)
    documentChangesDisposable = null
  }

  private fun startObserveFileEditorChanges() {
    logd()

    if (fileEditorChangesDisposable.isNotDisposed()) {
      logd("Already observing")
      return
    }

    val disposable = newDisposable()
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
    dispose(fileEditorChangesDisposable)
    fileEditorChangesDisposable = null
  }

  private fun startContinuousGradleCodegen() {
    logd()

    if (gradleCodegenCancellation != null) {
      logd("Already running")
      return
    }

    val modules = ModuleManager.getInstance(project).modules
    coroutineScope.launch {
      gradleCodegenCancellation = GradleConnector.newCancellationTokenSource()
      logd("Start Gradle")
      try {
        val cancellationToken = gradleCodegenCancellation!!.token()
        val gradleProjectPath = project.getGradleRootPath()
        if (gradleProjectPath == null) {
          logw("Could not get Gradle root project path")
          return@launch
        }
        runGradleBuild(project, gradleProjectPath) {
          it.forTasks(CODEGEN_GRADLE_TASK_NAME)
              .withCancellationToken(cancellationToken)
              .addArguments("--continuous")
              .let {
                if (project.projectSettingsState.automaticCodegenAdditionalGradleJvmArguments.isNotEmpty()) {
                  it.addJvmArguments(project.projectSettingsState.automaticCodegenAdditionalGradleJvmArguments.split(' '))
                } else {
                  it
                }
              }
              .addProgressListener(object : SimpleProgressListener() {
                override fun onSuccess() {
                  logd("Gradle build success, marking generated source roots as dirty")
                  // Mark the generated sources dirty so the files are visible to the IDE
                  val generatedSourceRoots = modules.flatMap { it.apolloGeneratedSourcesRoots() }
                  logd("Mark dirty $generatedSourceRoots")
                  VfsUtil.markDirtyAndRefresh(true, true, true, *generatedSourceRoots.toTypedArray())
                }
              })
        }
        logd("Gradle execution finished")
      } catch (t: Throwable) {
        logd(t, "Gradle execution failed")
      } finally {
        gradleCodegenCancellation = null
      }
    }
  }

  private fun stopContinuousGradleCodegen() {
    logd()
    gradleCodegenCancellation?.cancel()
    gradleCodegenCancellation = null
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
        stopContinuousGradleCodegen()
        if (shouldTriggerCodegenAutomatically()) startContinuousGradleCodegen()
      }
    })
  }

  private fun stopObserveGradleHasSynced() {
    logd()
    dispose(gradleHasSyncedDisposable)
    gradleHasSyncedDisposable = null
  }

  override fun dispose() {
    logd("project=${project.name}")
    stopContinuousGradleCodegen()
  }
}
