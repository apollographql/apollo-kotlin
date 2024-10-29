package com.apollographql.ijplugin.refactoring

import com.apollographql.ijplugin.navigation.findKotlinFragmentClassDefinitions
import com.apollographql.ijplugin.navigation.findKotlinOperationDefinitions
import com.apollographql.ijplugin.util.apolloKotlinService
import com.apollographql.ijplugin.util.capitalizeFirstLetter
import com.apollographql.ijplugin.util.cast
import com.intellij.lang.jsgraphql.psi.GraphQLElement
import com.intellij.lang.jsgraphql.psi.GraphQLFragmentDefinition
import com.intellij.lang.jsgraphql.psi.GraphQLIdentifier
import com.intellij.lang.jsgraphql.psi.GraphQLTypedOperationDefinition
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.refactoring.rename.RenameDialog
import com.intellij.refactoring.rename.RenamePsiElementProcessor

/**
 * Allows to rename the corresponding usage in Kotlin code when renaming a GraphQL operation or fragment definition.
 * The file name is also renamed if it matches the current operation or fragment name (as is customary).
 */
class GraphQLDefinitionRenameProcessor : RenamePsiElementProcessor() {
  private var newName: String = ""

  override fun canProcessElement(element: PsiElement): Boolean {
    return element is GraphQLIdentifier && (element.parent is GraphQLTypedOperationDefinition || element.parent is GraphQLFragmentDefinition)
  }

  override fun createRenameDialog(
      project: Project,
      element: PsiElement,
      nameSuggestionContext: PsiElement?,
      editor: Editor?,
  ): RenameDialog {
    if (element.parent !is GraphQLTypedOperationDefinition) {
      return super.createRenameDialog(project, element, nameSuggestionContext, editor)
    }
    return object : RenameDialog(project, element, nameSuggestionContext, editor) {
      override fun getFullName(): String {
        val definition = element.parent as GraphQLTypedOperationDefinition
        val type = definition.operationType.text
        return "$type '${definition.name}'"
      }


    }
  }

  override fun prepareRenaming(element: PsiElement, newName: String, allRenames: MutableMap<PsiElement, String>) {
    prepareRenamingFile(element, newName, allRenames)
    prepareRenamingKotlinClasses(element, newName, allRenames)
    this.newName = newName
  }

  private fun prepareRenamingFile(
      element: PsiElement,
      newName: String,
      allRenames: MutableMap<PsiElement, String>,
  ) {
    val file = element.containingFile ?: return
    val virtualFile = file.virtualFile ?: return
    val elementCurrentName = (element as GraphQLIdentifier).referenceName
    // Only rename the file if it previously had the same name as the element
    if (virtualFile.nameWithoutExtension == elementCurrentName) {
      allRenames[file] = newName + "." + virtualFile.extension
    }
  }

  private fun prepareRenamingKotlinClasses(
      element: PsiElement,
      newName: String,
      allRenames: MutableMap<PsiElement, String>,
  ) {
    when (val parent = element.parent) {
      is GraphQLTypedOperationDefinition -> {
        val kotlinDefinitions = findKotlinOperationDefinitions(parent)
        val useSemanticNaming = element.cast<GraphQLElement>()!!.apolloKotlinService()?.useSemanticNaming ?: true
        for (kotlinDefinition in kotlinDefinitions) {
          allRenames[kotlinDefinition] =
            newName.capitalizeFirstLetter() + if (useSemanticNaming) {
              val suffix = parent.operationType.text.capitalizeFirstLetter()
              if (!newName.endsWith(suffix)) {
                suffix
              } else {
                ""
              }
            } else {
              ""
            }
        }
      }

      is GraphQLFragmentDefinition -> {
        val kotlinDefinitions = findKotlinFragmentClassDefinitions(parent)
        for (kotlinDefinition in kotlinDefinitions) {
          allRenames[kotlinDefinition] = newName.capitalizeFirstLetter()
        }
      }
    }
  }
}
