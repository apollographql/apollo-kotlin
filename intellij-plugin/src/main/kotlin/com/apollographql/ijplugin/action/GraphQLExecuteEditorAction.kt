package com.apollographql.ijplugin.action

import com.apollographql.ijplugin.jsgraphql.ui.GraphQLUIProjectService
import com.apollographql.ijplugin.util.logd
import com.intellij.json.JsonFileType
import com.intellij.lang.jsgraphql.GraphQLFileType
import com.intellij.lang.jsgraphql.ui.GraphQLUIProjectService.GRAPH_QL_EDITOR_QUERYING
import com.intellij.lang.jsgraphql.ui.GraphQLUIProjectService.GRAPH_QL_QUERY_EDITOR
import com.intellij.lang.jsgraphql.ui.GraphQLUIProjectService.IS_GRAPH_QL_VARIABLES_VIRTUAL_FILE
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.components.service
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile

/**
 * This is identical to the GraphQL plugin's [com.intellij.lang.jsgraphql.ide.actions.GraphQLExecuteEditorAction] except that it calls
 * our version of [GraphQLUIProjectService] that strips Apollo client side directives before executing the query.
 */
class GraphQLExecuteEditorAction : com.intellij.lang.jsgraphql.ide.actions.GraphQLExecuteEditorAction() {
  override fun actionPerformed(e: AnActionEvent) {
    logd()
    var virtualFile = e.getData(CommonDataKeys.VIRTUAL_FILE)
    val project = e.getData(CommonDataKeys.PROJECT) ?: return
    if (!isQueryableFile(project, virtualFile)) {
      return
    }
    var editor: Editor = e.getData(CommonDataKeys.EDITOR) as? EditorEx ?: return
    if (editor.getUserData(GRAPH_QL_EDITOR_QUERYING) == true) {
      // Already doing a query
      return
    }
    val queryEditor = editor.getUserData(GRAPH_QL_QUERY_EDITOR)
    if (queryEditor != null) {
      // This action comes from the variables editor, so we need to resolve the query editor which contains the GraphQL
      editor = queryEditor
      virtualFile = CommonDataKeys.VIRTUAL_FILE.getData((editor as EditorEx).dataContext)
    }
    if (virtualFile == null) {
      return
    }
    project.service<GraphQLUIProjectService>().executeGraphQL(editor, virtualFile)
  }
}

private fun isQueryableFile(project: Project, virtualFile: VirtualFile?): Boolean {
  if (virtualFile == null) {
    return false
  }
  if (virtualFile.fileType == GraphQLFileType.INSTANCE) {
    return true
  }
  if (GraphQLFileType.isGraphQLScratchFile(virtualFile)) {
    return true
  }
  return virtualFile.fileType == JsonFileType.INSTANCE && virtualFile.getUserData(IS_GRAPH_QL_VARIABLES_VIRTUAL_FILE) == true
}
