package com.apollographql.ijplugin.refactoring.migration.v3tov4.item

import com.apollographql.ijplugin.refactoring.migration.item.MigrationItem
import com.apollographql.ijplugin.refactoring.migration.item.MigrationItemUsageInfo
import com.apollographql.ijplugin.util.apollo3
import com.apollographql.ijplugin.util.apollo4
import com.apollographql.ijplugin.util.findPsiFilesByName
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiMigration
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtNameReferenceExpression
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.psi.KtTreeVisitorVoid

object UpdateScalarAdaptersInBuildKts : MigrationItem() {
  override fun findUsages(project: Project, migration: PsiMigration, searchScope: GlobalSearchScope): List<MigrationItemUsageInfo> {
    val buildGradleKtsFiles: List<PsiFile> = project.findPsiFilesByName("build.gradle.kts", searchScope)
    val usages = mutableListOf<MigrationItemUsageInfo>()
    for (file in buildGradleKtsFiles) {
      if (file !is KtFile) continue
      file.accept(object : KtTreeVisitorVoid() {
        override fun visitCallExpression(expression: KtCallExpression) {
          super.visitCallExpression(expression)
          if ((expression.calleeExpression as? KtNameReferenceExpression)?.getReferencedName() == "mapScalar" && expression.valueArguments.size == 3) {
            val expressionArgument = expression.valueArguments[2].stringTemplateExpression?.text ?: return
            if (expressionArgument.contains(apollo3)) {
              usages.add(MigrationItemUsageInfo(this@UpdateScalarAdaptersInBuildKts, expression))
            }
          }
        }
      })
    }
    return usages
  }

  override fun performRefactoring(project: Project, migration: PsiMigration, usage: MigrationItemUsageInfo) {
    val element = usage.element
    val psiFactory = KtPsiFactory(project)
    element.replace(psiFactory.createExpression(element.text.replace(apollo3, apollo4)))
  }
}
