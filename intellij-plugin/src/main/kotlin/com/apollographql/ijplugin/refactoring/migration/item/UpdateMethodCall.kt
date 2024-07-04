package com.apollographql.ijplugin.refactoring.migration.item

import com.apollographql.ijplugin.refactoring.findMethodReferences
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiMigration
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.util.parentOfType
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression
import org.jetbrains.kotlin.psi.KtImportDirective
import org.jetbrains.kotlin.psi.KtPsiFactory

class UpdateMethodCall(
    private val containingDeclarationName: String,
    private val methodName: String,
    private val replacementExpression: String,
    private val extensionTargetClassName: String? = null,
    private vararg val importsToAdd: String,
) : MigrationItem(), DeletesElements {
  override fun findUsages(project: Project, migration: PsiMigration, searchScope: GlobalSearchScope): List<MigrationItemUsageInfo> {
    return findMethodReferences(
        project = project,
        className = containingDeclarationName,
        methodName = methodName,
        extensionTargetClassName = extensionTargetClassName
    ).toMigrationItemUsageInfo()
  }

  override fun performRefactoring(project: Project, migration: PsiMigration, usage: MigrationItemUsageInfo) {
    val element = usage.element
    val importDirective = element.parentOfType<KtImportDirective>()
    if (importDirective != null) {
      // Reference is an import
      importDirective.delete()
    } else {
      element.parentOfType<KtDotQualifiedExpression>()?.selectorExpression?.replace(KtPsiFactory(project).createExpression(replacementExpression))
    }
  }

  override fun importsToAdd() = importsToAdd.toSet()
}
