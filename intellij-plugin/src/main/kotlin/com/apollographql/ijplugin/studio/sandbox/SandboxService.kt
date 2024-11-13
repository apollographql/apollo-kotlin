package com.apollographql.ijplugin.studio.sandbox

import com.apollographql.ijplugin.util.logd
import com.intellij.lang.jsgraphql.GraphQLFileType
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.ActionToolbar
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.invokeLater
import com.intellij.openapi.components.Service
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.FileEditorManagerListener
import com.intellij.openapi.fileEditor.TextEditor
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.OnePixelSplitter
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstanceOrNull

@Service(Service.Level.PROJECT)
class SandboxService(
    private val project: Project,
) : Disposable {
  init {
    logd("project=${project.name}")

    // Add custom action on editor headers to already open files
    addOpenInSandboxActionToOpenedEditors()

    startObserveFileEditorChanges()
  }

  private fun addOpenInSandboxActionToOpenedEditors() {
    val fileEditorManager = FileEditorManager.getInstance(project)
    for (file in fileEditorManager.openFiles) {
      addOpenInSandboxAction(file)
    }
  }

  private fun startObserveFileEditorChanges() {
    logd()
    project.messageBus.connect(this).subscribe(
        FileEditorManagerListener.FILE_EDITOR_MANAGER,
        object : FileEditorManagerListener {
          override fun fileOpened(source: FileEditorManager, file: VirtualFile) {
            logd("$file")
            ApplicationManager.getApplication().executeOnPooledThread {
              invokeLater {
                addOpenInSandboxAction(file)
              }
            }
          }
        }
    )
  }

  private fun addOpenInSandboxAction(file: VirtualFile) {
    // Only care about GraphQL files
    if (!GraphQLFileType.isGraphQLFile(file)) return

    val fileEditorManager = FileEditorManager.getInstance(project)
    val fileEditor = fileEditorManager.getSelectedEditor(file) ?: return
    val editor = (fileEditor as? TextEditor)?.editor ?: return
    val existingEditorHeaderComponent = editor.headerComponent ?: return

    // XXX This is fragile as it tightly relies on how the header component's UI is built by the GraphQL Plugin
    val onePixelSplitter = existingEditorHeaderComponent.components?.firstIsInstanceOrNull<OnePixelSplitter>() ?: return
    val actionToolbar = onePixelSplitter.secondComponent as? ActionToolbar ?: return
    val actionGroup = actionToolbar.actionGroup as? DefaultActionGroup ?: return

    if (actionGroup.getChildActionsOrStubs().none { it is OpenInSandboxAction }) {
      actionGroup.addSeparator()
      actionGroup.addAction(ActionManager.getInstance().getAction(OpenInSandboxAction.ACTION_ID))
    }
  }

  override fun dispose() {
    logd("project=${project.name}")
  }
}
