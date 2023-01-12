package com.apollographql.ijplugin.refactoring.migration.item

import com.apollographql.ijplugin.refactoring.findMethodReferences
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiMigration
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.psi.KtPsiFactory

class UpdateMethodName(
    private val className: String,
    private val oldMethodName: String,
    private val newMethodName: String,
    private val importToAdd: String? = null,
) : MigrationItem() {
  override fun findUsages(project: Project, migration: PsiMigration, searchScope: GlobalSearchScope): List<MigrationItemUsageInfo> {
    return findMethodReferences(project = project, className = className, methodName = oldMethodName).toMigrationItemUsageInfo()
  }

  override fun performRefactoring(project: Project, migration: PsiMigration, usage: MigrationItemUsageInfo) {
    val newMethodReference = KtPsiFactory(project).createExpression(newMethodName)
    usage.element.replace(newMethodReference)
  }

  override fun importsToAdd() = if (importToAdd != null) setOf(importToAdd) else emptySet()
}
