package com.apollographql.ijplugin.refactoring.migration.v2tov3.item

import com.apollographql.ijplugin.refactoring.findMethodReferences
import com.apollographql.ijplugin.refactoring.migration.item.MigrationItem
import com.apollographql.ijplugin.refactoring.migration.item.MigrationItemUsageInfo
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiMigration
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.psi.KtPsiFactory

object UpdateInputAbsent : MigrationItem() {
  override fun findUsages(project: Project, migration: PsiMigration, searchScope: GlobalSearchScope): List<MigrationItemUsageInfo> {
    return findMethodReferences(project = project, className = "com.apollographql.apollo.api.Input.Companion", methodName = "absent")
        .map { it.element.parent.toMigrationItemUsageInfo() }

  }

  override fun performRefactoring(project: Project, migration: PsiMigration, usage: MigrationItemUsageInfo) {
    val newMethodReference = KtPsiFactory(project).createExpression("Absent")
    usage.element.replace(newMethodReference)
  }
}
