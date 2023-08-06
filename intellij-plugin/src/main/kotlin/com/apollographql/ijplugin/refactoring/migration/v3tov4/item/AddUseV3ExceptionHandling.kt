package com.apollographql.ijplugin.refactoring.migration.v3tov4.item

import com.apollographql.ijplugin.refactoring.findMethodReferences
import com.apollographql.ijplugin.refactoring.migration.item.MigrationItem
import com.apollographql.ijplugin.refactoring.migration.item.MigrationItemUsageInfo
import com.apollographql.ijplugin.refactoring.migration.item.toMigrationItemUsageInfo
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiMigration
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.psi.KtPsiFactory

object AddUseV3ExceptionHandling : MigrationItem() {
  override fun findUsages(project: Project, migration: PsiMigration, searchScope: GlobalSearchScope): List<MigrationItemUsageInfo> {
    return findMethodReferences(
        project = project,
        className = "com.apollographql.apollo3.ApolloClient.Builder",
        methodName = "Builder",
    )
        .toMigrationItemUsageInfo()
  }

  override fun performRefactoring(project: Project, migration: PsiMigration, usage: MigrationItemUsageInfo) {
    val parentExpression = usage.element.parent?.parent ?: return
    val replacedExpression = KtPsiFactory(project).createExpression(parentExpression.text.replace("Builder()", "Builder().useV3ExceptionHandling(true)"))
    parentExpression.replace(replacedExpression)
  }
}
