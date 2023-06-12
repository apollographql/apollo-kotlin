package com.apollographql.ijplugin.studio.sandbox

import com.apollographql.ijplugin.ApolloBundle
import com.apollographql.ijplugin.icons.ApolloIcons
import com.apollographql.ijplugin.util.logd
import com.intellij.ide.BrowserUtil
import com.intellij.lang.jsgraphql.GraphQLFileType
import com.intellij.lang.jsgraphql.ide.introspection.promptForEnvVariables
import com.intellij.lang.jsgraphql.ui.GraphQLUIProjectService
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys

class OpenInSandboxAction : AnAction(
    ApolloBundle.messagePointer("SandboxService.OpenInSandboxAction.text"),
    ApolloIcons.Action.Apollo
) {

  companion object {
    val ACTION_ID = OpenInSandboxAction::class.java.simpleName
  }

  override fun update(e: AnActionEvent) {
    // Only care about GraphQL files
    e.presentation.isEnabled = e.getData(CommonDataKeys.VIRTUAL_FILE)?.let { file ->
      e.project?.let { project ->
        GraphQLFileType.isGraphQLFile(project, file)
      }
    } == true
  }

  override fun actionPerformed(e: AnActionEvent) {
    logd()
    val project = e.project ?: return

    val editor = e.getData(CommonDataKeys.EDITOR) ?: return
    val contents = editor.document.text

    val endpointsModel = editor.getUserData(GraphQLUIProjectService.GRAPH_QL_ENDPOINTS_MODEL)
    val selectedEndpointUrl = endpointsModel?.let { promptForEnvVariables(project, it.selectedItem) }?.url

    val variablesEditor = editor.getUserData(GraphQLUIProjectService.GRAPH_QL_VARIABLES_EDITOR)
    val variables = variablesEditor?.document?.text

    // See https://www.apollographql.com/docs/graphos/explorer/sandbox/#url-parameters
    val url = buildString {
      append("https://studio.apollographql.com/sandbox/explorer?document=$contents")
      if (selectedEndpointUrl != null) {
        append("&endpoint=$selectedEndpointUrl")
      }
      if (!variables.isNullOrBlank()) {
        append("&variables=$variables")
      }
    }
    BrowserUtil.browse(url, project)
  }

  override fun getActionUpdateThread() = ActionUpdateThread.BGT
}
