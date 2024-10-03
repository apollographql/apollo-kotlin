package com.apollographql.ijplugin.refactoring.migration.v3tov4.item

import com.apollographql.ijplugin.refactoring.migration.item.MigrationItem
import com.apollographql.ijplugin.refactoring.migration.item.MigrationItemUsageInfo
import com.apollographql.ijplugin.util.containingKtFile
import com.apollographql.ijplugin.util.findPsiFilesByName
import com.apollographql.ijplugin.util.getMethodName
import com.apollographql.ijplugin.util.isMethodCall
import com.apollographql.ijplugin.util.lambdaBlockExpression
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiMigration
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.psi.KtTreeVisitorVoid

object UpdateMultiModuleConfiguration : MigrationItem() {
  override fun findUsages(project: Project, migration: PsiMigration, searchScope: GlobalSearchScope): List<MigrationItemUsageInfo> {
    val usages = mutableListOf<MigrationItemUsageInfo>()
    val buildGradleKtsFiles: List<KtFile> = project.findPsiFilesByName("build.gradle.kts", searchScope).filterIsInstance<KtFile>()
    for (file in buildGradleKtsFiles) {
      file.accept(object : KtTreeVisitorVoid() {
        override fun visitCallExpression(expression: KtCallExpression) {
          super.visitCallExpression(expression)
          if (expression.getMethodName() == "apolloMetadata") {
            val argumentText = expression.valueArguments.firstOrNull()?.text ?: return
            usages.add(expression.toMigrationItemUsageInfo(argumentText))
          }
        }
      })
    }
    return usages
  }

  override fun performRefactoring(project: Project, migration: PsiMigration, usage: MigrationItemUsageInfo) {
    val file = usage.element.containingKtFile() ?: return
    usage.element.delete()
    val ktFactory = KtPsiFactory(project)

    file.accept(object : KtTreeVisitorVoid() {
      override fun visitCallExpression(expression: KtCallExpression) {
        super.visitCallExpression(expression)
        var serviceFound = false
        if (expression.getMethodName() == "apollo") {
          val apolloBlockExpression = expression.lambdaBlockExpression() ?: return
          val apolloStatements = apolloBlockExpression.statements
          apolloStatements.filter { it.isMethodCall("service") }.forEach { serviceCallExpression ->
            val serviceBlockExpression = (serviceCallExpression as KtCallExpression).lambdaBlockExpression() ?: return@forEach
            serviceBlockExpression.add(ktFactory.createNewLine())
            serviceBlockExpression.add(ktFactory.createExpression("dependsOn(${usage.attachedData<String>()})"))
            serviceFound = true
          }
          if (!serviceFound) {
            apolloBlockExpression.add(ktFactory.createNewLine())
            apolloBlockExpression.add(ktFactory.createExpression("dependsOn(${usage.attachedData<String>()})"))
          }
        }
      }
    })
  }
}
