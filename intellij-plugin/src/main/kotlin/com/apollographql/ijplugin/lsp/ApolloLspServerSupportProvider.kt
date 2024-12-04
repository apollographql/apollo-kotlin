@file:Suppress("UnstableApiUsage")

package com.apollographql.ijplugin.lsp

import com.apollographql.ijplugin.file.ApolloGraphQLFileType
import com.apollographql.ijplugin.icons.ApolloIcons
import com.apollographql.ijplugin.rover.RoverHelper
import com.apollographql.ijplugin.settings.appSettingsState
import com.apollographql.ijplugin.settings.lsp.LspSettingsConfigurable
import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.platform.lsp.api.LspServer
import com.intellij.platform.lsp.api.LspServerManager
import com.intellij.platform.lsp.api.LspServerSupportProvider
import com.intellij.platform.lsp.api.ProjectWideLspServerDescriptor
import com.intellij.platform.lsp.api.lsWidget.LspServerWidgetItem

internal class ApolloLspServerSupportProvider : LspServerSupportProvider {
  override fun fileOpened(project: Project, file: VirtualFile, serverStarter: LspServerSupportProvider.LspServerStarter) {
    if (appSettingsState.lspModeEnabled && file.extension in ApolloGraphQLFileType.SUPPORTED_EXTENSIONS) {
      serverStarter.ensureServerStarted(ApolloLspServerDescriptor(project))
    }
  }

  override fun createLspServerWidgetItem(lspServer: LspServer, currentFile: VirtualFile?): LspServerWidgetItem {
    return LspServerWidgetItem(
        lspServer = lspServer,
        currentFile = currentFile,
        icon = ApolloIcons.StatusBar.Apollo,
        settingsPageClass = LspSettingsConfigurable::class.java,
    )
  }
}

private class ApolloLspServerDescriptor(project: Project) : ProjectWideLspServerDescriptor(project, "Apollo") {
  override fun isSupportedFile(file: VirtualFile) = file.extension in ApolloGraphQLFileType.SUPPORTED_EXTENSIONS
  override fun createCommandLine() = RoverHelper.getLspCommandLine(project)

  override fun getLanguageId(file: VirtualFile): String {
    return "graphql"
  }
}

fun restartApolloLsp() {
  runInEdt {
    for (project in ProjectManager.getInstance().openProjects) {
      LspServerManager.getInstance(project).stopAndRestartIfNeeded(ApolloLspServerSupportProvider::class.java)
    }
  }
}
