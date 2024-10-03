package com.apollographql.ijplugin.refactoring.migration.v2tov3.item

import com.apollographql.ijplugin.refactoring.migration.item.MigrationItem
import com.apollographql.ijplugin.refactoring.migration.item.MigrationItemUsageInfo
import com.apollographql.ijplugin.util.addSiblingAfter
import com.apollographql.ijplugin.util.findPsiFilesByName
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiMigration
import com.intellij.psi.PsiWhiteSpace
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.psi.KtTreeVisitorVoid

object AddUseVersion2Compat : MigrationItem() {
  override fun findUsages(project: Project, migration: PsiMigration, searchScope: GlobalSearchScope): List<MigrationItemUsageInfo> {
    val buildGradleKtsFiles: List<PsiFile> = project.findPsiFilesByName("build.gradle.kts", searchScope)
    val usages = mutableListOf<MigrationItemUsageInfo>()
    for (file in buildGradleKtsFiles) {
      if (file !is KtFile) continue
      file.accept(object : KtTreeVisitorVoid() {
        override fun visitCallExpression(expression: KtCallExpression) {
          super.visitCallExpression(expression)
          if (expression.calleeExpression?.text != "apollo") return
          val firstChild = (expression.lambdaArguments.firstOrNull()?.getLambdaExpression())?.functionLiteral?.lBrace
          firstChild?.let { usages.add(it.toMigrationItemUsageInfo()) }
        }
      })
    }
    return usages
  }

  override fun performRefactoring(project: Project, migration: PsiMigration, usage: MigrationItemUsageInfo) {
    val psiFactory = KtPsiFactory(project)
    val indent = (usage.element.nextSibling as? PsiWhiteSpace)?.text?.drop(1)?.length ?: 4
    val block = psiFactory.createBlockCodeFragment(
        """
      |
      |// TODO: This shortcut jumpstarts the migration from v2 to v3, but it is recommended to use settings idiomatic to v3 instead.
      |// See https://www.apollographql.com/docs/kotlin/migration/3.0/
      |useVersion2Compat()
      |
      |"""
            .trimMargin()
            .prependIndent(" ".repeat(indent)),
        usage.element.parent
    )
    usage.element.addSiblingAfter(block)
  }
}
