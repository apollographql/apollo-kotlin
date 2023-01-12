package com.apollographql.ijplugin.services.impl

import com.apollographql.ijplugin.services.ApolloProjectService
import com.apollographql.ijplugin.util.getGradleName
import com.apollographql.ijplugin.util.isApolloAndroid2Project
import com.apollographql.ijplugin.util.isApolloKotlin3Project
import com.apollographql.ijplugin.util.logd
import com.apollographql.ijplugin.util.logw
import com.apollographql.ijplugin.util.runWriteActionInEdt
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
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessModuleDir
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.openapi.vfs.newvfs.BulkFileListener
import com.intellij.openapi.vfs.newvfs.events.VFileDeleteEvent
import com.intellij.openapi.vfs.newvfs.events.VFileEvent
import com.intellij.psi.PsiDocumentManager
import org.gradle.tooling.CancellationTokenSource
import org.gradle.tooling.GradleConnector
import org.jetbrains.kotlin.idea.framework.GRADLE_SYSTEM_ID
import org.jetbrains.plugins.gradle.service.execution.GradleExecutionHelper
import org.jetbrains.plugins.gradle.settings.GradleExecutionSettings
import org.jetbrains.plugins.gradle.util.GradleConstants

private const val CODEGEN_GRADLE_TASK_NAME = "generateApolloSources"

class ApolloProjectServiceImpl(
    private val project: Project,
) : ApolloProjectService, Disposable {

  // TODO: This is initialized only once, but this could actually change during the project's lifecycle
  // TODO: find a way to invalidate this whenever project's dependencies change
  override val isApolloAndroid2Project by lazy { project.isApolloAndroid2Project }
  override val isApolloKotlin3Project by lazy { project.isApolloKotlin3Project }

  private var dirtyGqlDocument: Document? = null

  private var codegenGradleCancellationTokenSource: CancellationTokenSource? = null

  init {
    logd("project=${project.name} isApolloKotlin3Project=$isApolloKotlin3Project")
    if (isApolloKotlin3Project) {
      // Trigger the codegen whenever a GraphQL file is saved.
      observeVfsChanges()

      // To make this more reactive, any touched GraphQL document will automatically be saved (thus triggering the codegen)
      // as soon as the current editor is changed.
      observeDocumentChanges()
      observeFileEditorChanges()
    }
  }

  private fun observeDocumentChanges() {
    EditorFactory.getInstance().eventMulticaster.addDocumentListener(object : DocumentListener {
      override fun documentChanged(event: DocumentEvent) {
        val vFile = PsiDocumentManager.getInstance(project).getPsiFile(event.document)?.virtualFile ?: return
        logd("vFile=${vFile.path}")
        val module = getModuleForGqlFile(vFile)
        if (module == null) {
          // Not a GraphQL file or not from this project: ignore
          return
        }
        dirtyGqlDocument = event.document
      }
    }, this)
  }

  private fun observeFileEditorChanges() {
    project.messageBus.connect(this).subscribe(FileEditorManagerListener.FILE_EDITOR_MANAGER, object : FileEditorManagerListener {
      override fun selectionChanged(event: FileEditorManagerEvent) {
        logd(event.newFile)
        dirtyGqlDocument?.let {
          dirtyGqlDocument = null
          runWriteActionInEdt {
            runCatching {
              FileDocumentManager.getInstance().saveDocument(it)
            }
          }
        }
      }
    })
  }

  private fun observeVfsChanges() {
    project.messageBus.connect().subscribe(
        VirtualFileManager.VFS_CHANGES,
        object : BulkFileListener {
          override fun before(events: MutableList<out VFileEvent>) {
            handleEvents(events.filterIsInstance<VFileDeleteEvent>())
          }

          override fun after(events: MutableList<out VFileEvent>) {
            handleEvents(events.filterNot { it is VFileDeleteEvent })
          }

          private fun handleEvents(events: List<VFileEvent>) {
            val modules = mutableSetOf<Module>()
            for (event in events) {
              val vFile = event.file!!
              logd("vFile=${vFile.path}")
              val module = getModuleForGqlFile(vFile)
              if (module == null) {
                // Not a GraphQL file or not from this project: ignore
                continue
              }
              modules += module
            }
            if (modules.isNotEmpty()) {
              triggerCodegen(modules)
            }
          }
        },
    )
  }

  private fun getModuleForGqlFile(vFile: VirtualFile): Module? {
    if (vFile.fileType !is GraphQLFileType) {
      // Only care for GraphQL files
      return null
    }
    val moduleForFile = ProjectRootManager.getInstance(project).fileIndex.getModuleForFile(vFile)
    if (moduleForFile == null) {
      // A file from an external project: ignore
      return null
    }
    return moduleForFile
  }

  private fun triggerCodegen(modules: Set<Module>) {
    logd("modules=$modules")

    // Cancel any already running codegen task
    codegenGradleCancellationTokenSource?.cancel()

    val rootProjectPath = ExternalSystemApiUtil.getExternalRootProjectPath(modules.first())
    if (rootProjectPath == null) {
      logw("Could not get root project path for module ${modules.first().name}")
      return
    }
    val taskNames = modules.map {
      val gradleModuleName = it.getGradleName()
      if (gradleModuleName == "") CODEGEN_GRADLE_TASK_NAME else ":$gradleModuleName:$CODEGEN_GRADLE_TASK_NAME"
    }
    logd("taskNames=$taskNames")
    val executionSettings = ExternalSystemApiUtil.getExecutionSettings<GradleExecutionSettings>(
        project,
        rootProjectPath,
        GradleConstants.SYSTEM_ID
    )

    val gradleExecutionHelper = GradleExecutionHelper()
    gradleExecutionHelper.execute(rootProjectPath, executionSettings) { connection ->
      codegenGradleCancellationTokenSource = GradleConnector.newCancellationTokenSource()

      try {
        val id = ExternalSystemTaskId.create(GRADLE_SYSTEM_ID, ExternalSystemTaskType.REFRESH_TASKS_LIST, project)
        gradleExecutionHelper.getBuildLauncher(
            id,
            connection,
            executionSettings,
            ExternalSystemTaskNotificationListenerAdapter.NULL_OBJECT
        )
            .forTasks(*taskNames.toTypedArray())
            .withCancellationToken(codegenGradleCancellationTokenSource!!.token())
            .run()
        logd("Codegen task finished successfully")

        // Sync impacted modules so the generated files are visible to the IDE
        for (module in modules) {
          VfsUtil.markDirtyAndRefresh(true, true, true, module.guessModuleDir())
        }
      } catch (t: Throwable) {
        logd(t, "Failed to run codegen")
      }
    }
  }

  override fun dispose() {
    logd("project=${project.name}")
  }
}
