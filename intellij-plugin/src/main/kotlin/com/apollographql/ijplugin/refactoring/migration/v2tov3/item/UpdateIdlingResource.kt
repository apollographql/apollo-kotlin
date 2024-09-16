package com.apollographql.ijplugin.refactoring.migration.v2tov3.item

import com.apollographql.ijplugin.refactoring.findMethodReferences
import com.apollographql.ijplugin.refactoring.migration.item.MigrationItem
import com.apollographql.ijplugin.refactoring.migration.item.MigrationItemUsageInfo
import com.apollographql.ijplugin.util.addSiblingBefore
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiMigration
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.util.parentOfType
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.psi.createExpressionByPattern

object UpdateIdlingResource : MigrationItem() {
  override fun findUsages(project: Project, migration: PsiMigration, searchScope: GlobalSearchScope): List<MigrationItemUsageInfo> {
    return findMethodReferences(project, "com.apollographql.apollo.test.espresso.ApolloIdlingResource", "create")
        .mapNotNull {
          it.element.parentOfType<KtDotQualifiedExpression>()?.toMigrationItemUsageInfo()
        }
  }

  override fun performRefactoring(project: Project, migration: PsiMigration, usage: MigrationItemUsageInfo) {
    val element = usage.element as KtDotQualifiedExpression
    val psiFactory = KtPsiFactory(project)
    val argumentValue = (element.selectorExpression as? KtCallExpression)?.valueArguments?.firstOrNull()?.text ?: return
    val newElement = psiFactory.createExpressionByPattern("ApolloIdlingResource($0)", argumentValue)
    element.addSiblingBefore(psiFactory.createComment("// TODO: Set the ApolloIdlingResource on the ApolloClient. See https://www.apollographql.com/docs/kotlin/migration/3.0/#idlingresource"))
    element.replace(newElement)
  }
}
