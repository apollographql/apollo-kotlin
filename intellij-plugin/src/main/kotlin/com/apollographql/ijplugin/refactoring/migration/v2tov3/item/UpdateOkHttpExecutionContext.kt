package com.apollographql.ijplugin.refactoring.migration.v2tov3.item

import com.apollographql.ijplugin.refactoring.findClassReferences
import com.apollographql.ijplugin.refactoring.findOrCreateClass
import com.apollographql.ijplugin.refactoring.migration.item.MigrationItem
import com.apollographql.ijplugin.refactoring.migration.item.MigrationItemUsageInfo
import com.apollographql.ijplugin.util.apollo3
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMigration
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.util.parentOfType
import org.jetbrains.kotlin.psi.KtImportDirective
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.psi.psiUtil.getParentOfTypesAndPredicate
import org.jetbrains.kotlin.resolve.ImportPath

object UpdateOkHttpExecutionContext : MigrationItem() {
  override fun prepare(project: Project, migration: PsiMigration) {
    findOrCreateClass(project, migration, "$apollo3.network.http.HttpInfo")
  }

  override fun findUsages(project: Project, migration: PsiMigration, searchScope: GlobalSearchScope): List<MigrationItemUsageInfo> {
    return findClassReferences(project, "com.apollographql.apollo.http.OkHttpExecutionContext")
        .mapNotNull {
          val element = it.element
          val importDirective = element.parentOfType<KtImportDirective>()
          if (importDirective != null) {
            // Reference is an import
            ReplaceImportUsageInfo(this@UpdateOkHttpExecutionContext, importDirective)
          } else {
            val parent = element.getParentOfTypesAndPredicate(false, PsiElement::class.java) { parent ->
              parent.text.matches(Regex(".+\\.response.?\\.(code|headers)\\(\\)"))
            }
            parent?.toMigrationItemUsageInfo()
          }
        }

  }

  private class ReplaceImportUsageInfo(migrationItem: MigrationItem, element: KtImportDirective) :
    MigrationItemUsageInfo(migrationItem, element)


  override fun performRefactoring(project: Project, migration: PsiMigration, usage: MigrationItemUsageInfo) {
    val element = usage.element
    val psiFactory = KtPsiFactory(project)
    when (usage) {
      is ReplaceImportUsageInfo -> {
        element.replace(psiFactory.createImportDirective(ImportPath.fromString("$apollo3.network.http.HttpInfo")))
      }

      else -> {
        val newExpression = element.text
            .replace("OkHttpExecutionContext", "HttpInfo")
            .replace(Regex("response.?\\.code\\(\\)"), "statusCode")
            .replace(Regex("response.?\\.headers\\(\\)"), "headers")
        element.replace(psiFactory.createExpression(newExpression))
      }
    }
  }
}
