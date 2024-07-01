package com.apollographql.ijplugin.refactoring.migration.v2tov3.item

import com.apollographql.ijplugin.refactoring.findClassReferences
import com.apollographql.ijplugin.refactoring.migration.item.MigrationItem
import com.apollographql.ijplugin.refactoring.migration.item.MigrationItemUsageInfo
import com.apollographql.ijplugin.refactoring.migration.item.toMigrationItemUsageInfo
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiMigration
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.util.parentOfType
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.refactoring.fqName.fqName
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtImportDirective
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode

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
            if (callExpression.valueArguments.size == 2) {
              val expression = callExpression.valueArguments[1]?.getArgumentExpression()
              if (expression?.analyze(BodyResolveMode.PARTIAL)?.getType(expression)?.fqName?.asString() == "kotlin.String") {
                callExpression
              } else {
                null
              }
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
