package com.apollographql.ijplugin.refactoring.migration.v2tov3.item

import com.apollographql.ijplugin.refactoring.migration.item.MigrationItem
import com.apollographql.ijplugin.refactoring.migration.item.MigrationItemUsageInfo
import com.apollographql.ijplugin.util.findPsiFilesByName
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiMigration
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.psi.KtBinaryExpression
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.psi.KtReferenceExpression
import org.jetbrains.kotlin.psi.KtTreeVisitorVoid
import org.jetbrains.kotlin.psi.createExpressionByPattern

object UpdateGraphqlSourceDirectorySet : MigrationItem() {
  override fun findUsages(project: Project, migration: PsiMigration, searchScope: GlobalSearchScope): List<MigrationItemUsageInfo> {
    val buildGradleKtsFiles: List<PsiFile> = project.findPsiFilesByName("build.gradle.kts", searchScope)
    val usages = mutableListOf<MigrationItemUsageInfo>()
    for (file in buildGradleKtsFiles) {
      if (file !is KtFile) continue
      file.accept(object : KtTreeVisitorVoid() {
        override fun visitReferenceExpression(expression: KtReferenceExpression) {
          super.visitReferenceExpression(expression)
          if (expression.text == "graphqlSourceDirectorySet") {
            val dotQualifiedExpression = expression.parent as? KtDotQualifiedExpression ?: return
            (dotQualifiedExpression.parent as? KtBinaryExpression)?.let {
              usages.add(it.toMigrationItemUsageInfo())
              return
            }

            val calleeText = (dotQualifiedExpression.selectorExpression as? KtCallExpression)?.calleeExpression?.text ?: return
            if (calleeText in setOf("include", "exclude")) {
              usages.add(dotQualifiedExpression.toMigrationItemUsageInfo(calleeText))
            }
          }
        }
      })
    }
    return usages
  }

  override fun performRefactoring(project: Project, migration: PsiMigration, usage: MigrationItemUsageInfo) {
    val psiFactory = KtPsiFactory(project)
    val element = usage.element
    val newElement = if (element is KtBinaryExpression) {
      psiFactory.createExpressionByPattern("srcDir($0)", element.right!!.text)
    } else {
      element as KtDotQualifiedExpression
      val fieldName = if (usage.attachedData<String>() == "include") "includes" else "excludes"
      val argumentText = (element.selectorExpression as? KtCallExpression)?.valueArguments?.firstOrNull()?.text ?: return
      psiFactory.createExpressionByPattern("$0.add($1)", fieldName, argumentText)
    }
    element.replace(newElement)
  }
}
