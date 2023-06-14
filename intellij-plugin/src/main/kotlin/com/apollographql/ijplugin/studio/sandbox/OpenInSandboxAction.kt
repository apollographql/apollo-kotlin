package com.apollographql.ijplugin.studio.sandbox

import com.apollographql.ijplugin.ApolloBundle
import com.apollographql.ijplugin.icons.ApolloIcons
import com.apollographql.ijplugin.util.logd
import com.intellij.ide.BrowserUtil
import com.intellij.lang.jsgraphql.GraphQLFileType
import com.intellij.lang.jsgraphql.ide.config.model.GraphQLConfigEndpoint
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
    val isGraphQLFile = e.getData(CommonDataKeys.VIRTUAL_FILE)?.let { file ->
      e.project?.let { project ->
        GraphQLFileType.isGraphQLFile(project, file)
      }
    } == true
    e.presentation.isEnabled = isGraphQLFile

    // Only show the action if there's an editor (i.e. not in 'Open in' from the project view)
    e.presentation.isVisible = isGraphQLFile || e.getData(CommonDataKeys.EDITOR) != null
  }

  override fun actionPerformed(e: AnActionEvent) {
    logd()
    val project = e.project ?: return

    // Editor will be present if the action is triggered from the editor toolbar, the main menu, the Open In popup inside the editor
    // Otherwise it will be null, and we fallback to the File (but no endpoint / variables)
    val editor = e.getData(CommonDataKeys.EDITOR)
    val contents = editor?.document?.text ?: run {
      val file = e.getData(CommonDataKeys.VIRTUAL_FILE) ?: return@actionPerformed
      file.contentsToByteArray().toString(file.charset)
    }

    val endpointsModel = editor?.getUserData(GraphQLUIProjectService.GRAPH_QL_ENDPOINTS_MODEL)
    val graphQLConfigEndpoint: GraphQLConfigEndpoint? = endpointsModel?.let { promptForEnvVariables(project, it.selectedItem) }
    val selectedEndpointUrl = graphQLConfigEndpoint?.url
    val headers = graphQLConfigEndpoint?.headers?.formatAsJson()

    val variablesEditor = editor?.getUserData(GraphQLUIProjectService.GRAPH_QL_VARIABLES_EDITOR)
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
      if (!headers.isNullOrBlank()) {
        append("&headers=$headers")
      }
    }
    BrowserUtil.browse(url, project)
  }

  override fun getActionUpdateThread() = ActionUpdateThread.BGT
}

private fun Map<String, Any?>.formatAsJson() = "{" + map { (key, value) -> """"$key": "$value"""" }.joinToString() + "}"
