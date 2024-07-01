package com.apollographql.ijplugin.refactoring.migration.v2tov3.item

import com.apollographql.ijplugin.refactoring.findClassReferences
import com.apollographql.ijplugin.refactoring.migration.item.MigrationItem
import com.apollographql.ijplugin.refactoring.migration.item.MigrationItemUsageInfo
import com.apollographql.ijplugin.util.apollo3
import com.apollographql.ijplugin.util.cast
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMigration
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.util.parentOfType
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression
import org.jetbrains.kotlin.psi.KtImportDirective
import org.jetbrains.kotlin.psi.KtNameReferenceExpression
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.psi.psiUtil.findDescendantOfType
import org.jetbrains.kotlin.resolve.ImportPath

object UpdateLruNormalizedCacheFactory : MigrationItem() {
  private const val CACHE_FACTORY_FQN = "com.apollographql.apollo.cache.normalized.lru.LruNormalizedCacheFactory"

  override fun findUsages(project: Project, migration: PsiMigration, searchScope: GlobalSearchScope): List<MigrationItemUsageInfo> {
    return findClassReferences(project, CACHE_FACTORY_FQN)
        .mapNotNull {
          val element = it.element
          val importDirective = element.parentOfType<KtImportDirective>()
          if (importDirective != null) {
            // Reference is an import
            ReplaceImportUsageInfo(this@UpdateLruNormalizedCacheFactory, importDirective)
          } else {
            // Reference is a class reference
            // Looking for something like:
            //     EvictionPolicy.builder()
            //      .maxSizeBytes(10 * 1024 * 1024)
            //      .expireAfterWrite(10, TimeUnit.MILLISECONDS)
            //      .build()
            val callExpression = element.parent as? KtCallExpression ?: return@mapNotNull null
            val argumentExpression = callExpression.valueArguments.first().getArgumentExpression() as? KtDotQualifiedExpression
                ?: return@mapNotNull null

            var maxSizeBytesValue: String? = null
            val maxSizeBytesCall =
                argumentExpression.findDescendantOfType<KtNameReferenceExpression> { it.getReferencedName() == "maxSizeBytes" }
            if (maxSizeBytesCall != null) {
              maxSizeBytesValue = maxSizeBytesCall.parent.cast<KtCallExpression>()?.valueArguments?.firstOrNull()?.text
            }

            var expireAfterWriteTimeValue: String? = null
            var expireAfterWriteUnitValue: String? = null
            val expireAfterWriteCall =
                argumentExpression.findDescendantOfType<KtNameReferenceExpression> { it.getReferencedName() == "expireAfterWrite" }
            if (expireAfterWriteCall != null) {
              val arguments = expireAfterWriteCall.parent.cast<KtCallExpression>()?.valueArguments
              expireAfterWriteTimeValue = arguments?.firstOrNull()?.text
              expireAfterWriteUnitValue = arguments?.getOrNull(1)?.text
            }

            val arguments = mutableListOf<String>()
            if (maxSizeBytesValue != null) {
              arguments.add("maxSizeBytes = $maxSizeBytesValue")
            }
            if (expireAfterWriteTimeValue != null && expireAfterWriteUnitValue?.startsWith("TimeUnit.") == true) {
              arguments.add("expireAfterMillis = $expireAfterWriteUnitValue.toMillis($expireAfterWriteTimeValue)")
            }

            val replaceExpressionUsageInfo = ReplaceExpressionUsageInfo(
                this@UpdateLruNormalizedCacheFactory,
                element.parent,
                "MemoryCacheFactory(${arguments.joinToString(", ")})"
            )
            replaceExpressionUsageInfo
          }
        }
  }

  private class ReplaceImportUsageInfo(migrationItem: MigrationItem, element: KtImportDirective) :
      MigrationItemUsageInfo(migrationItem, element)

  private class ReplaceExpressionUsageInfo(migrationItem: MigrationItem, element: PsiElement, val replacementExpression: String) :
      MigrationItemUsageInfo(migrationItem, element)

  override fun performRefactoring(project: Project, migration: PsiMigration, usage: MigrationItemUsageInfo) {
    val element = usage.element
    val psiFactory = KtPsiFactory(project)
    when (usage) {
      is ReplaceImportUsageInfo -> {
        element.replace(psiFactory.createImportDirective(ImportPath.fromString("$apollo3.cache.normalized.api.MemoryCacheFactory")))
      }

      is ReplaceExpressionUsageInfo -> {
        element.replace(psiFactory.createExpression(usage.replacementExpression))
      }
    }
  }

  override fun importsToAdd() = setOf("$apollo3.cache.normalized.normalizedCache")
}
