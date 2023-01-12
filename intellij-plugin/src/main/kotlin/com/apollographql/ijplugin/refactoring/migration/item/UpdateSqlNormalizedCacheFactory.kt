package com.apollographql.ijplugin.refactoring.migration.item

import com.apollographql.ijplugin.refactoring.findClassReferences
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiMigration
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.util.parentOfType
import org.jetbrains.kotlin.idea.debugger.sequence.psi.resolveType
import org.jetbrains.kotlin.idea.refactoring.fqName.fqName
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtImportDirective

object UpdateSqlNormalizedCacheFactory : MigrationItem() {
  private const val CACHE_FACTORY_FQN = "com.apollographql.apollo.cache.normalized.sql.SqlNormalizedCacheFactory"

  override fun findUsages(project: Project, migration: PsiMigration, searchScope: GlobalSearchScope): List<MigrationItemUsageInfo> {
    return findClassReferences(
        project,
        CACHE_FACTORY_FQN,
    )
        .mapNotNull {
          val element = it.element
          val importDirective = element.parentOfType<KtImportDirective>()
          if (importDirective != null) {
            // Reference is an import
            null
          } else {
            // `SqlNormalizedCacheFactory(...)`
            val callExpression = element.parent as? KtCallExpression ?: return@mapNotNull null
            // `SqlNormalizedCacheFactory(xxx, yyy)` and yyy is a String
            if (callExpression.valueArguments.size == 2 &&
                callExpression.valueArguments[1]?.getArgumentExpression()?.resolveType()?.fqName?.asString() == "kotlin.String"
            ) {
              callExpression
            } else {
              null
            }
          }
        }
        .map { it.toMigrationItemUsageInfo() }
  }

  override fun performRefactoring(project: Project, migration: PsiMigration, usage: MigrationItemUsageInfo) {
    (usage.element as KtCallExpression).valueArgumentList?.removeArgument(0)
  }

  override fun importsToAdd() = setOf("com.apollographql.apollo3.cache.normalized.normalizedCache")
}
