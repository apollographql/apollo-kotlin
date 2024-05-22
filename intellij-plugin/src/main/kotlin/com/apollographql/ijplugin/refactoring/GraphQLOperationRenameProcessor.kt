package com.apollographql.ijplugin.refactoring

import com.intellij.lang.jsgraphql.psi.GraphQLIdentifier
import com.intellij.lang.jsgraphql.psi.GraphQLTypedOperationDefinition
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.refactoring.rename.RenameDialog
import com.intellij.refactoring.rename.RenamePsiElementProcessor

class GraphQLOperationRenameProcessor : RenamePsiElementProcessor() {
  override fun canProcessElement(element: PsiElement): Boolean {
    return element is GraphQLIdentifier && element.parent is GraphQLTypedOperationDefinition
  }

  override fun createRenameDialog(
      project: Project,
      element: PsiElement,
      nameSuggestionContext: PsiElement?,
      editor: Editor?,
  ): RenameDialog {
    return object : RenameDialog(project, element, nameSuggestionContext, editor) {
      override fun getFullName(): String {
        val definition = element.parent as GraphQLTypedOperationDefinition
        val type = definition.operationType.text
        return "$type '${definition.name}'"
      }
    }
  }

  override fun prepareRenaming(element: PsiElement, newName: String, allRenames: MutableMap<PsiElement, String>) {
    prepareRenamingFile(element, allRenames, newName)
  }

  private fun prepareRenamingFile(
      element: PsiElement,
      allRenames: MutableMap<PsiElement, String>,
      newName: String,
  ) {
    val file = element.containingFile ?: return
    val virtualFile = file.virtualFile ?: return
    val elementCurrentName = (element as GraphQLIdentifier).referenceName
    if (virtualFile.nameWithoutExtension == elementCurrentName) {
      allRenames[file] = newName + "." + virtualFile.extension
    }
  }
}
