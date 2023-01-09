package com.apollographql.ijplugin.refactoring.migration.item

import com.apollographql.ijplugin.refactoring.findMethodReferences
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiMigration
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.util.parentOfType
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression
import org.jetbrains.kotlin.psi.KtImportDirective

open class RemoveMethodCall(
    private val containingDeclarationName: String,
    private val methodName: String,
    private val extensionTargetClassName: String? = null,
    private val removeImportsOnly: Boolean = false,
) : MigrationItem(), DeletesElements {
  override fun findUsages(project: Project, migration: PsiMigration, searchScope: GlobalSearchScope): List<MigrationItemUsageInfo> {
    return findMethodReferences(
        project = project,
        className = containingDeclarationName,
        methodName = methodName,
        extensionTargetClassName = extensionTargetClassName,
    )
        .filter {
          if (removeImportsOnly) {
            it.element.parentOfType<KtImportDirective>() != null
          } else {
            true
          }
        }
        .toMigrationItemUsageInfo()
  }

  override fun performRefactoring(project: Project, migration: PsiMigration, usage: MigrationItemUsageInfo) {
    val element = usage.element
    val importDirective = element.parentOfType<KtImportDirective>()
    if (importDirective != null) {
      // Reference is an import
      importDirective.delete()
    } else {
      if (removeImportsOnly) return
      val dotQualifiedExpression = element.parentOfType<KtDotQualifiedExpression>()
      if (dotQualifiedExpression != null) {
        // Reference is a method call
        // <some expression>.<await()> -> <some expression>
        dotQualifiedExpression.replace(dotQualifiedExpression.receiverExpression)
      }
    }
  }
}
