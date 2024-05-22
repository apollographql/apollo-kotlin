package com.apollographql.ijplugin.refactoring

import com.apollographql.ijplugin.navigation.compat.KotlinFindUsagesHandlerFactoryCompat
import com.apollographql.ijplugin.navigation.findKotlinFragmentClassDefinitions
import com.apollographql.ijplugin.navigation.findKotlinOperationDefinitions
import com.apollographql.ijplugin.util.isGenerated
import com.intellij.lang.jsgraphql.psi.GraphQLFragmentDefinition
import com.intellij.lang.jsgraphql.psi.GraphQLIdentifier
import com.intellij.lang.jsgraphql.psi.GraphQLTypedOperationDefinition
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReference
import com.intellij.psi.search.SearchScope
import com.intellij.refactoring.rename.RenameDialog
import com.intellij.refactoring.rename.RenamePsiElementProcessor
import com.intellij.usageView.UsageInfo
import com.intellij.util.Processor

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
    prepareRenamingFile(element, allRenames, newName)
    this.newName = newName
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

  override fun findReferences(
      element: PsiElement,
      searchScope: SearchScope,
      searchInCommentsAndStrings: Boolean,
  ): Collection<PsiReference> {
    val references = mutableListOf<PsiReference>()
    val kotlinDefinitions = when (val parent = element.parent) {
      is GraphQLTypedOperationDefinition -> {
        if (!(newName.endsWith("Query") || newName.endsWith("Mutation") || newName.endsWith("Subscription"))) {
          // When useSemanticNaming is true (the default), renaming e.g. FooQuery to FooQuery2 will generate FooQuery2Query.
          // For now we'll only support the happy case, and won't try to rename references otherwise.
          // TODO: We could support this by looking at the value of useSemanticNaming from the Gradle Tooling Model, and implementing
          // the same naming logic as the Apollo compiler.
          return super.findReferences(element, searchScope, searchInCommentsAndStrings)
        }

        findKotlinOperationDefinitions(parent)
      }

      is GraphQLFragmentDefinition -> {
        findKotlinFragmentClassDefinitions(parent)
      }

      else -> emptyList()
    }.ifEmpty {
      return super.findReferences(element, searchScope, searchInCommentsAndStrings)
    }

    val kotlinFindUsagesHandlerFactory = KotlinFindUsagesHandlerFactoryCompat(element.project)
    val processor = object : Processor<UsageInfo> {
      override fun process(t: UsageInfo): Boolean {
        if (t.virtualFile?.isGenerated(t.project) != true) {
          t.reference?.let { references.add(it) }
        }
        return true
      }
    }
    for (kotlinDefinition in kotlinDefinitions) {
      if (kotlinFindUsagesHandlerFactory.canFindUsages(kotlinDefinition)) {
        val kotlinFindUsagesHandler = kotlinFindUsagesHandlerFactory.createFindUsagesHandler(kotlinDefinition, false)
            ?: break
        val findUsageOptions = kotlinFindUsagesHandlerFactory.findClassOptions ?: break
        kotlinFindUsagesHandler.processElementUsages(kotlinDefinition, processor, findUsageOptions)
      }
    }

    return super.findReferences(element, searchScope, searchInCommentsAndStrings) + references
  }
}
