package com.apollographql.ijplugin.refactoring.migration.v3tov4.item

import com.apollographql.ijplugin.refactoring.findMethodReferences
import com.apollographql.ijplugin.refactoring.migration.apollo3
import com.apollographql.ijplugin.refactoring.migration.item.MigrationItem
import com.apollographql.ijplugin.refactoring.migration.item.MigrationItemUsageInfo
import com.apollographql.ijplugin.refactoring.migration.item.toMigrationItemUsageInfo
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiMigration
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.util.parentOfType
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression
import org.jetbrains.kotlin.psi.KtImportDirective
import org.jetbrains.kotlin.psi.KtPsiFactory

object ReplaceExecuteCacheAndNetwork : MigrationItem() {
  override fun findUsages(project: Project, migration: PsiMigration, searchScope: GlobalSearchScope): List<MigrationItemUsageInfo> {
    return findMethodReferences(
        project = project,
        className = "$apollo3.cache.normalized.NormalizedCache",
        methodName = "executeCacheAndNetwork",
        extensionTargetClassName = "$apollo3.ApolloCall"
    ).toMigrationItemUsageInfo()
  }

  override fun performRefactoring(project: Project, migration: PsiMigration, usage: MigrationItemUsageInfo) {
   val element = usage.element
    val importDirective = element.parentOfType<KtImportDirective>()
    if (importDirective != null) {
      // Reference is an import
      return
    }
    element.parentOfType<KtDotQualifiedExpression>()?.selectorExpression?.replace(KtPsiFactory(project).createExpression("fetchPolicy(FetchPolicy.CacheAndNetwork).toFlow()"))
  }

  override fun importsToAdd() = setOf("$apollo3.cache.normalized.fetchPolicy", "$apollo3.cache.normalized.FetchPolicy")
}
