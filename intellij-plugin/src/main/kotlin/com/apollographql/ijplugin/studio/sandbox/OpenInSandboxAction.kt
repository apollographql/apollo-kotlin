package com.apollographql.ijplugin.studio.sandbox

import com.apollographql.ijplugin.ApolloBundle
import com.apollographql.ijplugin.icons.ApolloIcons
import com.apollographql.ijplugin.telemetry.TelemetryEvent
import com.apollographql.ijplugin.telemetry.telemetryService
import com.apollographql.ijplugin.util.logd
import com.apollographql.ijplugin.util.urlEncoded
import com.intellij.ide.BrowserUtil
import com.intellij.lang.jsgraphql.GraphQLFileType
import com.intellij.lang.jsgraphql.ide.config.model.GraphQLConfigEndpoint
import com.intellij.lang.jsgraphql.ide.introspection.promptForEnvVariables
import com.intellij.lang.jsgraphql.psi.GraphQLFragmentDefinition
import com.intellij.lang.jsgraphql.psi.GraphQLFragmentSpread
import com.intellij.lang.jsgraphql.psi.GraphQLRecursiveVisitor
import com.intellij.lang.jsgraphql.ui.GraphQLUIProjectService
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.intellij.psi.util.parentOfType

class OpenInSandboxAction : AnAction(
    ApolloBundle.messagePointer("SandboxService.OpenInSandboxAction.text"),
    ApolloIcons.Action.Apollo
) {

  companion object {
    val ACTION_ID: String = OpenInSandboxAction::class.java.simpleName
  }

  override fun update(e: AnActionEvent) {
    // Only care about GraphQL files
    val isGraphQLFile = e.getData(CommonDataKeys.VIRTUAL_FILE)?.let { file ->
      e.project?.let { project ->
        GraphQLFileType.isGraphQLFile(file)
      }
    } == true
    e.presentation.isEnabled = isGraphQLFile

    // Only show the action if there's an editor (i.e. not in 'Open in' from the project view)
    e.presentation.isVisible = isGraphQLFile || e.getData(CommonDataKeys.EDITOR) != null
  }

  override fun actionPerformed(e: AnActionEvent) {
    logd()
    val project = e.project ?: return
    project.telemetryService.logEvent(TelemetryEvent.ApolloIjOpenInApolloSandbox())

    // Editor will be present if the action is triggered from the editor toolbar, the main menu, the Open In popup inside the editor
    // Otherwise it will be null, and we fallback to the File (but no endpoint / variables)
    val editor = e.getData(CommonDataKeys.EDITOR)
    val psiFile = editor?.document?.let { PsiDocumentManager.getInstance(project).getPsiFile(it) }
        ?: e.getData(CommonDataKeys.VIRTUAL_FILE)?.let { virtualFile ->
          PsiManager.getInstance(project).findFile(virtualFile)
        }
        ?: return
    val contents = contentsWithReferencedFragments(psiFile).urlEncoded()

    val endpointsModel = editor?.getUserData(GraphQLUIProjectService.GRAPH_QL_ENDPOINTS_MODEL)
    val graphQLConfigEndpoint: GraphQLConfigEndpoint? = endpointsModel?.let { promptForEnvVariables(project, it.selectedItem) }
    val selectedEndpointUrl = graphQLConfigEndpoint?.url?.urlEncoded()
    val headers = graphQLConfigEndpoint?.headers?.formatAsJson()?.urlEncoded()

    val variablesEditor = editor?.getUserData(GraphQLUIProjectService.GRAPH_QL_VARIABLES_EDITOR)
    val variables = variablesEditor?.document?.text?.urlEncoded()

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

  /**
   * Get contents of the file, including all referenced fragments, recursively, if they belong to a different file
   */
  private fun contentsWithReferencedFragments(psiFile: PsiFile, fragmentDefinition: GraphQLFragmentDefinition? = null): String {
    val contents = StringBuilder(fragmentDefinition?.text ?: psiFile.text)
    val visitor = object : GraphQLRecursiveVisitor() {
      override fun visitFragmentSpread(o: GraphQLFragmentSpread) {
        super.visitFragmentSpread(o)
        val referencedFragmentDefinition = o.nameIdentifier.reference?.resolve()?.parentOfType<GraphQLFragmentDefinition>() ?: return
        if (referencedFragmentDefinition.containingFile != psiFile) {
          contents.append("\n\n# From ${referencedFragmentDefinition.containingFile.virtualFile.name}\n")
          contents.append(contentsWithReferencedFragments(referencedFragmentDefinition.containingFile, referencedFragmentDefinition))
        }
      }
    }
    if (fragmentDefinition != null) {
      visitor.visitFragmentDefinition(fragmentDefinition)
    } else {
      visitor.visitFile(psiFile)
    }
    return contents.toString()
  }

  override fun getActionUpdateThread() = ActionUpdateThread.BGT
}

private fun Map<String, Any?>.formatAsJson() = "{" + map { (key, value) -> """"$key": "$value"""" }.joinToString() + "}"
