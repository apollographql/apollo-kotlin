package com.apollographql.ijplugin.refactoring.migration.v3tov4.item

import com.apollographql.ijplugin.refactoring.findMethodReferences
import com.apollographql.ijplugin.refactoring.migration.item.MigrationItem
import com.apollographql.ijplugin.refactoring.migration.item.MigrationItemUsageInfo
import com.apollographql.ijplugin.util.apollo3
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiMigration
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.util.parentOfType
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtImportDirective

object RemoveWatchMethodArguments : MigrationItem() {
  override fun findUsages(project: Project, migration: PsiMigration, searchScope: GlobalSearchScope): List<MigrationItemUsageInfo> {
    return findMethodReferences(
        project = project,
        className = "$apollo3.cache.normalized.NormalizedCache",
        methodName = "watch",
        extensionTargetClassName = "$apollo3.ApolloCall",
    ) { method -> method.parameterList.parameters.any { it.name == "fetchThrows" } }
        .toMigrationItemUsageInfo()
  }

  override fun performRefactoring(project: Project, migration: PsiMigration, usage: MigrationItemUsageInfo) {
    val element = usage.element
    val importDirective = element.parentOfType<KtImportDirective>()
    if (importDirective != null) {
      // Reference is an import
      return
    }
    val methodCall = element.parentOfType<KtCallExpression>() ?: return
    val valueArgumentList = methodCall.valueArgumentList ?: return
    repeat(valueArgumentList.arguments.size) {
      valueArgumentList.removeArgument(0)
    }
  }
}
