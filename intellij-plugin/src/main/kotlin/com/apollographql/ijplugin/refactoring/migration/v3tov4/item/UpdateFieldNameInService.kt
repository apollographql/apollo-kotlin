package com.apollographql.ijplugin.refactoring.migration.v3tov4.item

import com.apollographql.ijplugin.refactoring.migration.item.MigrationItem
import com.apollographql.ijplugin.refactoring.migration.item.MigrationItemUsageInfo
import com.apollographql.ijplugin.util.cast
import com.apollographql.ijplugin.util.findPsiFilesByName
import com.apollographql.ijplugin.util.getMethodName
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiMigration
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtNameReferenceExpression
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.psi.KtTreeVisitorVoid

class UpdateFieldNameInService(
    val oldName: String,
    val newName: String,
) : MigrationItem() {
  override fun findUsages(project: Project, migration: PsiMigration, searchScope: GlobalSearchScope): List<MigrationItemUsageInfo> {
    val usages = mutableListOf<MigrationItemUsageInfo>()
    val buildGradleKtsFiles: List<KtFile> = project.findPsiFilesByName("build.gradle.kts", searchScope).filterIsInstance<KtFile>()
    for (file in buildGradleKtsFiles) {
      file.accept(object : KtTreeVisitorVoid() {
        override fun visitCallExpression(expression: KtCallExpression) {
          super.visitCallExpression(expression)
          if (expression.getMethodName() == "apollo") {
            expression.accept(object : KtTreeVisitorVoid() {
              override fun visitDotQualifiedExpression(expression: KtDotQualifiedExpression) {
                super.visitDotQualifiedExpression(expression)
                if (expression.receiverExpression.cast<KtNameReferenceExpression>()?.getReferencedName() == oldName) {
                  usages.add(expression.receiverExpression.toMigrationItemUsageInfo())
                }
              }
            })
          }
        }
      })
    }
    return usages
  }

  override fun performRefactoring(project: Project, migration: PsiMigration, usage: MigrationItemUsageInfo) {
    val element = usage.element
    element.replace(KtPsiFactory(project).createExpression(newName))
  }
}
