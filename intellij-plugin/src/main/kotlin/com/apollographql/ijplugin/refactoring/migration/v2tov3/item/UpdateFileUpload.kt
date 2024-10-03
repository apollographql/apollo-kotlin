package com.apollographql.ijplugin.refactoring.migration.v2tov3.item

import com.apollographql.ijplugin.refactoring.findClassReferences
import com.apollographql.ijplugin.refactoring.migration.item.MigrationItem
import com.apollographql.ijplugin.refactoring.migration.item.MigrationItemUsageInfo
import com.apollographql.ijplugin.util.apollo3
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiMigration
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.psi.createExpressionByPattern

object UpdateFileUpload : MigrationItem() {
  override fun findUsages(project: Project, migration: PsiMigration, searchScope: GlobalSearchScope): List<MigrationItemUsageInfo> {
    return findClassReferences(project, "com.apollographql.apollo.api.FileUpload")
        .mapNotNull {
          (it.element.parent as? KtCallExpression)?.toMigrationItemUsageInfo()
        }

  }

  override fun performRefactoring(project: Project, migration: PsiMigration, usage: MigrationItemUsageInfo) {
    val element = usage.element as KtCallExpression
    val psiFactory = KtPsiFactory(project)
    val newElement =
      psiFactory.createExpressionByPattern("File($0).toUpload($1)", element.valueArguments[1].text, element.valueArguments[0].text)
    element.replace(newElement)
  }

  override fun importsToAdd(): Set<String> {
    return setOf(
        "java.io.File",
        "$apollo3.api.toUpload",
    )
  }
}
