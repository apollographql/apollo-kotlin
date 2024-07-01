package com.apollographql.ijplugin.refactoring.migration.v2tov3.item

import com.apollographql.ijplugin.refactoring.findMethodReferences
import com.apollographql.ijplugin.refactoring.migration.item.DeletesElements
import com.apollographql.ijplugin.refactoring.migration.item.MigrationItem
import com.apollographql.ijplugin.refactoring.migration.item.MigrationItemUsageInfo
import com.apollographql.ijplugin.util.apollo3
import com.apollographql.ijplugin.util.resolve
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMigration
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtNameReferenceExpression
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.KtPsiFactory

object UpdateHttpCache : MigrationItem(), DeletesElements {
  override fun findUsages(project: Project, migration: PsiMigration, searchScope: GlobalSearchScope): List<MigrationItemUsageInfo> {
    return findMethodReferences(
        project = project,
        className = "com.apollographql.apollo.ApolloClient.Builder",
        methodName = "httpCache"
    )
        .flatMap {
          val element = it.element
          // `httpCache(...)`
          val httpCacheCallExpression = element.parent as? KtCallExpression ?: return@flatMap emptyList<MigrationItemUsageInfo>()
          val httpCacheArgumentExpression = httpCacheCallExpression.valueArguments.firstOrNull()?.getArgumentExpression()
              ?: return@flatMap emptyList<MigrationItemUsageInfo>()
          val replaceUsageInfos = mutableSetOf<ReplaceUsageInfo>()
          val elementsToDelete = mutableSetOf<PsiElement>()
          when (httpCacheArgumentExpression) {
            // `httpCache(ApolloHttpCache(...))`
            is KtCallExpression -> {
              val apolloHttpCacheArguments = extractApolloHttpCacheArguments(httpCacheArgumentExpression, elementsToDelete)
              if (apolloHttpCacheArguments != null) {
                replaceUsageInfos += ReplaceUsageInfo(
                    this@UpdateHttpCache,
                    element.parent,
                    "httpCache(${apolloHttpCacheArguments.joinToString(", ")})"
                )
              }
            }

            is KtNameReferenceExpression -> {
              // `httpCache(xxx)` where xxx is a val defined as `val xxx = ApolloHttpCache(...)`
              val referencedVal = httpCacheArgumentExpression.resolve()
              if (referencedVal is KtProperty) {
                val initializerExpression = referencedVal.initializer
                if (initializerExpression is KtCallExpression) {
                  val apolloHttpCacheArguments = extractApolloHttpCacheArguments(initializerExpression, elementsToDelete)
                  if (apolloHttpCacheArguments != null) {
                    replaceUsageInfos += ReplaceUsageInfo(
                        this@UpdateHttpCache,
                        element.parent,
                        "httpCache(${apolloHttpCacheArguments.joinToString(", ")})"
                    )
                    elementsToDelete += referencedVal
                  }
                }
              }
            }

            else -> {
              // Not supported, add a TODO comment
              replaceUsageInfos += ReplaceUsageInfo(
                  this@UpdateHttpCache,
                  element.parent,
                  "httpCache(/* TODO: This could not be migrated automatically. Please check the migration guide at https://www.apollographql.com/docs/kotlin/migration/3.0/ */)"
              )
            }
          }
          val deleteUsageInfos = elementsToDelete.map { elementToDelete ->
            DeleteUsageInfo(
                this@UpdateHttpCache,
                elementToDelete
            )
          }
          replaceUsageInfos + deleteUsageInfos
        }
  }

  private fun extractApolloHttpCacheArguments(callExpression: KtCallExpression, elementsToDelete: MutableSet<PsiElement>): List<String>? {
    // Look for `ApolloHttpCache(...)`
    if (callExpression.calleeExpression?.text == "ApolloHttpCache") {
      val apolloHttpCacheCtorArgument = callExpression.valueArguments.firstOrNull()?.getArgumentExpression() ?: return null
      // Look for `DiskLruHttpCacheStore(...)` call
      when (apolloHttpCacheCtorArgument) {
        is KtCallExpression -> {
          return extractDiskLruHttpCacheStoreArguments(apolloHttpCacheCtorArgument)
        }

        is KtNameReferenceExpression -> {
          // `ApolloHttpCache(xxx)` where xxx is a val defined as `val xxx = DiskLruHttpCacheStore(...)`
          val referencedVal = apolloHttpCacheCtorArgument.resolve()
          if (referencedVal is KtProperty) {
            val initializerExpression = referencedVal.initializer
            if (initializerExpression is KtCallExpression) {
              elementsToDelete += referencedVal
              return extractDiskLruHttpCacheStoreArguments(initializerExpression)
            }
          }
        }
      }
    }
    return null
  }

  private fun extractDiskLruHttpCacheStoreArguments(initializerExpression: KtCallExpression): List<String>? {
    if (initializerExpression.calleeExpression?.text == "DiskLruHttpCacheStore") {
      // There are 2 variants of `DiskLruHttpCacheStore` constructor: with 3 and 2 arguments
      val diskLruHttpCacheStore = if (initializerExpression.valueArguments.size == 3) {
        // Ignore the first argument (FileSystem)
        initializerExpression.valueArguments.drop(1)
      } else {
        initializerExpression.valueArguments
      }
      if (diskLruHttpCacheStore.size == 2) {
        val fileArgumentExpression = diskLruHttpCacheStore[0].getArgumentExpression()?.text
        val maxSizeArgumentExpression = diskLruHttpCacheStore[1].getArgumentExpression()?.text
        if (fileArgumentExpression != null && maxSizeArgumentExpression != null) {
          return listOf(fileArgumentExpression, maxSizeArgumentExpression)
        }
      }
    }
    return null
  }

  private class ReplaceUsageInfo(migrationItem: MigrationItem, element: PsiElement, val replacementExpression: String) :
      MigrationItemUsageInfo(migrationItem, element)

  private class DeleteUsageInfo(migrationItem: MigrationItem, element: PsiElement) : MigrationItemUsageInfo(migrationItem, element)

  override fun performRefactoring(project: Project, migration: PsiMigration, usage: MigrationItemUsageInfo) {
    val element = usage.element
    when (usage) {
      is ReplaceUsageInfo -> {
        val psiFactory = KtPsiFactory(project)
        val newMethodReference = psiFactory.createExpression(usage.replacementExpression)
        element.replace(newMethodReference)
      }

      is DeleteUsageInfo -> element.delete()
    }
  }

  override fun importsToAdd() = setOf("$apollo3.cache.http.httpCache")
}
