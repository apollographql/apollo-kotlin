package com.apollographql.ijplugin.refactoring.migration.item

import com.apollographql.ijplugin.refactoring.findFieldReferences
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiMigration
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.psi.KtPsiFactory

class UpdateFieldName(
    private val className: String,
    private val oldFieldName: String,
    private val newFieldName: String,
    private val isMethodCall: Boolean = false,
) : MigrationItem() {
  override fun findUsages(project: Project, migration: PsiMigration, searchScope: GlobalSearchScope): List<MigrationItemUsageInfo> {
    return findFieldReferences(project = project, className = className, fieldName = oldFieldName).toMigrationItemUsageInfo()
  }

  override fun performRefactoring(project: Project, migration: PsiMigration, usage: MigrationItemUsageInfo) {
    val newFieldReference = KtPsiFactory(project).createExpression(if (isMethodCall) "$newFieldName()" else newFieldName)
    usage.element.replace(newFieldReference)
  }
}
