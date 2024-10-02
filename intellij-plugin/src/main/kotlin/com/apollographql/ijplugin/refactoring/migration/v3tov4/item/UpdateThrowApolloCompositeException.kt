package com.apollographql.ijplugin.refactoring.migration.v3tov4.item

import com.apollographql.ijplugin.refactoring.findClassReferences
import com.apollographql.ijplugin.refactoring.findOrCreateClass
import com.apollographql.ijplugin.refactoring.migration.item.MigrationItem
import com.apollographql.ijplugin.refactoring.migration.item.MigrationItemUsageInfo
import com.apollographql.ijplugin.util.apollo3
import com.apollographql.ijplugin.util.apollo4
import com.apollographql.ijplugin.util.cast
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiMigration
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtPsiFactory

object UpdateThrowApolloCompositeException : MigrationItem() {
  override fun prepare(project: Project, migration: PsiMigration) {
    findOrCreateClass(project, migration, "$apollo3.exception.ApolloCompositeException")
  }

  override fun findUsages(project: Project, migration: PsiMigration, searchScope: GlobalSearchScope): List<MigrationItemUsageInfo> {
    return findClassReferences(project, "$apollo3.exception.ApolloCompositeException")
        .toMigrationItemUsageInfo()
        .filter { it.element.parent.cast<KtCallExpression>()?.valueArguments?.size == 2 }
        .map { it.element.parent.toMigrationItemUsageInfo() }
  }

  override fun performRefactoring(project: Project, migration: PsiMigration, usage: MigrationItemUsageInfo) {
    val element = usage.element
    val psiFactory = KtPsiFactory(project)
    val (arg1, arg2) = element.cast<KtCallExpression>()!!.valueArguments.map { it.getArgumentExpression()!!.text }
    element.replace(psiFactory.createExpression("DefaultApolloException(cause = $arg1).apply { addSuppressed($arg2) }"))
  }

  override fun importsToAdd() = setOf("$apollo4.exception.DefaultApolloException")
}
