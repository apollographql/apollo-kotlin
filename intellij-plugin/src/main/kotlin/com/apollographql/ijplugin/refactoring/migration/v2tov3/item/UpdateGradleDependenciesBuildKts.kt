package com.apollographql.ijplugin.refactoring.migration.v2tov3.item

import com.apollographql.ijplugin.refactoring.migration.item.MigrationItem
import com.apollographql.ijplugin.refactoring.migration.item.MigrationItemUsageInfo
import com.apollographql.ijplugin.refactoring.migration.item.toMigrationItemUsageInfo
import com.apollographql.ijplugin.util.findPsiFilesByName
import com.apollographql.ijplugin.util.unquoted
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiMigration
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtLiteralStringTemplateEntry
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.psi.KtStringTemplateExpression
import org.jetbrains.kotlin.psi.KtTreeVisitorVoid
import org.jetbrains.kotlin.psi.KtValueArgument

class UpdateGradleDependenciesBuildKts(
    private val oldGroupId: String,
    private val newGroupId: String,
) : MigrationItem() {
  override fun findUsages(project: Project, migration: PsiMigration, searchScope: GlobalSearchScope): List<MigrationItemUsageInfo> {
    val buildGradleKtsFiles: List<PsiFile> = project.findPsiFilesByName("build.gradle.kts", searchScope)
    val usages = mutableListOf<MigrationItemUsageInfo>()
    for (file in buildGradleKtsFiles) {
      if (file !is KtFile) continue
      file.accept(object : KtTreeVisitorVoid() {
        override fun visitLiteralStringTemplateEntry(entry: KtLiteralStringTemplateEntry) {
          super.visitLiteralStringTemplateEntry(entry)
          if (entry.text.startsWith(oldGroupId)) {
            val callExpression = entry.parent.parent.parent.parent as? KtCallExpression
            if (callExpression?.calleeExpression?.text in listOf("implementation", "api", "testImplementation", "testApi")) {
              usages.add(callExpression!!.toMigrationItemUsageInfo())
            }
          }
        }
      })
    }
    return usages
  }

  override fun performRefactoring(project: Project, migration: PsiMigration, usage: MigrationItemUsageInfo) {
    val argument = (usage.element as KtCallExpression).valueArguments.first()
    val entries = ((argument as KtValueArgument).getArgumentExpression() as? KtStringTemplateExpression)?.entries ?: return
    val firstEntry = entries.firstOrNull() ?: return
    val artifactId = firstEntry.text.unquoted().split(":")[1]
    firstEntry.replace(KtPsiFactory(project).createLiteralStringTemplateEntry("$newGroupId:$artifactId"))
    // Remove other entries (with recent versions of the Apollo Gradle plugin, there is no need to specify the version)
    entries.drop(1).forEach { it.delete() }
  }
}
