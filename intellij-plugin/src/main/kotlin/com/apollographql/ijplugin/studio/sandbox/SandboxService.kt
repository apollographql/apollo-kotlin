package com.apollographql.ijplugin.studio.sandbox

import com.apollographql.ijplugin.util.logd
import com.intellij.lang.jsgraphql.GraphQLFileType
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.actionSystem.impl.ActionToolbarImpl
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.FileEditorManagerListener
import com.intellij.openapi.fileEditor.TextEditor
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.OnePixelSplitter
import org.jetbrains.kotlin.idea.util.application.invokeLater
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstanceOrNull

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
            invokeLater {
              addOpenInSandboxAction(file)
            }
          }
        }
    )
  }

  private fun addOpenInSandboxAction(file: VirtualFile) {
    // Only care about GraphQL files
    if (!GraphQLFileType.isGraphQLFile(project, file)) return

    val fileEditorManager = FileEditorManager.getInstance(project)
    val fileEditor = fileEditorManager.getSelectedEditor(file) ?: return
    val editor = (fileEditor as? TextEditor)?.editor ?: return
    val existingEditorHeaderComponent = editor.headerComponent ?: return

    // XXX This is fragile as it tightly relies on how the header component's UI is built by the GraphQL Plugin
    val onePixelSplitter = existingEditorHeaderComponent.components?.firstIsInstanceOrNull<OnePixelSplitter>() ?: return
    val actionToolbar = onePixelSplitter.secondComponent as? ActionToolbarImpl ?: return
    val actionGroup = actionToolbar.actionGroup as? DefaultActionGroup ?: return

    if (!actionGroup.getChildActionsOrStubs().any { it is OpenInSandboxAction }) {
      actionGroup.addSeparator()
      actionGroup.addAction(OpenInSandboxAction())
    }
  }

  override fun dispose() {
    logd("project=${project.name}")
  }
}
