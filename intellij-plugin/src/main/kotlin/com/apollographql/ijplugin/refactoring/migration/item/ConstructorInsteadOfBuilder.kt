package com.apollographql.ijplugin.refactoring.migration.item

import com.apollographql.ijplugin.refactoring.findMethodReferences
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiMigration
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.util.parentOfType
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression

class ConstructorInsteadOfBuilder(
    private val containingDeclarationName: String,
    private val methodName: String,
) : MigrationItem() {
  override fun findUsages(project: Project, migration: PsiMigration, searchScope: GlobalSearchScope): List<MigrationItemUsageInfo> {
    return findMethodReferences(
        project = project,
        className = containingDeclarationName,
        methodName = methodName,
    ).toMigrationItemUsageInfo()
  }

  override fun performRefactoring(project: Project, migration: PsiMigration, usage: MigrationItemUsageInfo) {
    val element = usage.element
    val dotQualifiedExpression = element.parentOfType<KtDotQualifiedExpression>()?: return
    // myObj.method(x, y) -> myObj.myObj(x, y)
    (dotQualifiedExpression.selectorExpression as? KtCallExpression)?.calleeExpression?.replace(dotQualifiedExpression.receiverExpression)
    // myObj.myObj(x, y) -> myObj(x, y)
    dotQualifiedExpression.selectorExpression?.let { dotQualifiedExpression.replace(it) }
  }
}
