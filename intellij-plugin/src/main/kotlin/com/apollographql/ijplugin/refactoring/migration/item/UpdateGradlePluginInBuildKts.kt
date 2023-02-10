package com.apollographql.ijplugin.refactoring.migration.item

import com.apollographql.ijplugin.util.findPsiFilesByName
import com.apollographql.ijplugin.util.quoted
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiMigration
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.psi.KtBinaryExpression
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtLiteralStringTemplateEntry
import org.jetbrains.kotlin.psi.KtNameReferenceExpression
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.psi.KtStringTemplateExpression
import org.jetbrains.kotlin.psi.KtTreeVisitorVoid

class UpdateGradlePluginInBuildKts(
    private val oldPluginId: String,
    private val newPluginId: String,
    private val newPluginVersion: String,
) : MigrationItem() {
  override fun findUsages(project: Project, migration: PsiMigration, searchScope: GlobalSearchScope): List<MigrationItemUsageInfo> {
    val buildGradleKtsFiles: List<PsiFile> = project.findPsiFilesByName("build.gradle.kts", searchScope)
    val usages = mutableListOf<MigrationItemUsageInfo>()
    for (file in buildGradleKtsFiles) {
      if (file !is KtFile) continue
      file.accept(object : KtTreeVisitorVoid() {
        override fun visitCallExpression(expression: KtCallExpression) {
          super.visitCallExpression(expression)
          if ((expression.calleeExpression as? KtNameReferenceExpression)?.getReferencedName() == "id") {
            // id("xxx")
            val dependencyText =
                (expression.valueArguments.firstOrNull()?.getArgumentExpression() as? KtStringTemplateExpression)?.entries?.first()?.text
            if (dependencyText == oldPluginId) {
              val parent = expression.parent
              if (parent is KtBinaryExpression || parent is KtDotQualifiedExpression) {
                // id("xxx") version yyy  /  id("xxx").version(yyy)
                usages.add(parent.toMigrationItemUsageInfo())
              } else {
                usages.add(expression.toMigrationItemUsageInfo())
              }
            }
          }
        }
      })
    }
    return usages
  }

  override fun performRefactoring(project: Project, migration: PsiMigration, usage: MigrationItemUsageInfo) {
    when (val element = usage.element) {
      is KtBinaryExpression -> {
        // id("xxx") version yyy
        // If yyy is a simple String (a hardcoded version), replace it with the new version, otherwise leave it alone but add a comment
        val versionStringTemplateEntries = (element.right as? KtStringTemplateExpression)?.entries
        val versionIsHardcoded = versionStringTemplateEntries?.size == 1 && versionStringTemplateEntries[0] is KtLiteralStringTemplateEntry
        if (versionIsHardcoded) {
          element.replace(KtPsiFactory(project).createExpression("""id("$newPluginId") version "$newPluginVersion""""))
        } else {
          // Replace id
          (element.left as? KtCallExpression)?.valueArguments?.firstOrNull()?.replace(
              KtPsiFactory(project).createExpression(newPluginId.quoted())
          )
          // Add comment
          val comment = KtPsiFactory(project).createComment("// TODO: Update version to $newPluginVersion")
          element.parent.addBefore(comment, element)
        }
      }

      is KtDotQualifiedExpression -> {
        // id("xxx").version(yyy)
        // If yyy is a simple String (a hardcoded version), replace it with the new version, otherwise leave it alone but add a comment
        val versionCallExpression = element.selectorExpression as? KtCallExpression
        val versionStringTemplateEntries = (versionCallExpression?.valueArgumentList?.arguments?.firstOrNull()
            ?.children?.firstOrNull() as? KtStringTemplateExpression)?.entries
        val versionIsHardcoded = versionStringTemplateEntries?.size == 1 && versionStringTemplateEntries[0] is KtLiteralStringTemplateEntry
        if (versionIsHardcoded) {
          element.replace(KtPsiFactory(project).createExpression("""id("$newPluginId").version("$newPluginVersion")"""))
        } else {
          // Replace id
          (element.receiverExpression as? KtCallExpression)?.valueArguments?.firstOrNull()
              ?.replace(KtPsiFactory(project).createExpression(newPluginId.quoted()))
          // Add comment
          val comment = KtPsiFactory(project).createComment("// TODO: Update version to $newPluginVersion")
          element.parent.addBefore(comment, element)
        }
      }

      else -> {
        // id("xxx")
        element.replace(KtPsiFactory(project).createExpression("""id("$newPluginId")"""))
      }
    }
  }
}
