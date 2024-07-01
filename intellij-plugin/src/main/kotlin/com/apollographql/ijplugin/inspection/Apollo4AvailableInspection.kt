package com.apollographql.ijplugin.inspection

import com.apollographql.ijplugin.ApolloBundle
import com.apollographql.ijplugin.action.ApolloV3ToV4MigrationAction
import com.apollographql.ijplugin.project.apolloProjectService
import com.apollographql.ijplugin.refactoring.migration.v3tov4.ApolloV3ToV4MigrationProcessor
import com.apollographql.ijplugin.telemetry.TelemetryEvent
import com.apollographql.ijplugin.telemetry.telemetryService
import com.apollographql.ijplugin.util.getMethodName
import com.apollographql.ijplugin.util.unquoted
import com.intellij.codeInsight.intention.preview.IntentionPreviewInfo
import com.intellij.codeInsight.intention.preview.IntentionPreviewUtils
import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import org.jetbrains.kotlin.idea.util.application.isUnitTestMode
import org.jetbrains.kotlin.psi.KtBinaryExpression
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression
import org.jetbrains.kotlin.psi.KtLiteralStringTemplateEntry
import org.jetbrains.kotlin.psi.KtStringTemplateEntry
import org.jetbrains.kotlin.psi.KtStringTemplateExpression
import org.toml.lang.psi.TomlInlineTable
import org.toml.lang.psi.TomlLiteral
import org.toml.lang.psi.TomlTable
import org.toml.lang.psi.ext.TomlLiteralKind
import org.toml.lang.psi.ext.kind

private const val apollo3 = "com.apollographql.apollo"

class Apollo4AvailableInspection : LocalInspectionTool() {
  // XXX kts files are not highlighted in tests
  private val buildGradleFileName = if (isUnitTestMode()) "build.gradle.kt" else "build.gradle.kts"

  // Do not warn until 4.0 is stable (but keep enabled always in unit tests)
  private val isEnabled = isUnitTestMode() || !ApolloV3ToV4MigrationProcessor.apollo4LatestVersion.contains('-')

  override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
    return object : PsiElementVisitor() {
      private val registeredTomlVersionValues = mutableSetOf<PsiElement>()

      override fun visitElement(element: PsiElement) {
        if (!isEnabled) return
        if (!element.project.apolloProjectService.apolloVersion.isAtLeastV3) return
        when {
          element.containingFile.name.endsWith(".versions.toml") && element is TomlLiteral -> {
            visitVersionsToml(element, holder)
          }

          element.containingFile.name == buildGradleFileName && element is KtCallExpression -> {
            visitBuildGradleKts(element, holder)
          }
        }
      }

      private fun visitVersionsToml(element: TomlLiteral, holder: ProblemsHolder) {
        if (element.kind !is TomlLiteralKind.String) return
        val dependencyText = element.text.unquoted()
        if (dependencyText == apollo3 || dependencyText.startsWith("$apollo3:")) {
          // Find the associated version
          val versionEntry = (element.parent.parent as? TomlInlineTable)?.entries
              ?.first { it.key.text == "version" || it.key.text == "version.ref" } ?: return
          if (versionEntry.key.text == "version") {
            val version = versionEntry.value?.firstChild?.text?.unquoted() ?: return
            if (!version.startsWith("4")) {
              holder.registerProblem(element.parent.parent.parent, ApolloBundle.message("inspection.apollo4Available.reportText"), Apollo4AvailableQuickFix)
            }
          } else {
            // Resolve the reference
            val versionsTable = element.containingFile.children.filterIsInstance<TomlTable>()
                .firstOrNull { it.header.key?.text == "versions" } ?: return
            val versionRefKey = versionEntry.value?.text?.unquoted()
            val refTarget = versionsTable.entries.firstOrNull { it.key.text == versionRefKey } ?: return
            val version = refTarget.value?.firstChild?.text?.unquoted() ?: return
            if (!version.startsWith("4")) {
              // Do not highlight the same element several times
              if (refTarget.value!! !in registeredTomlVersionValues) {
                holder.registerProblem(refTarget.value!!, ApolloBundle.message("inspection.apollo4Available.reportText"), Apollo4AvailableQuickFix)
                registeredTomlVersionValues.add(refTarget.value!!)
              }
            }
          }
        }
      }

      private fun visitBuildGradleKts(callExpression: KtCallExpression, holder: ProblemsHolder) {
        when (callExpression.getMethodName()) {
          "id" -> {
            // id("xxx")
            val dependencyText = callExpression.getArgumentAsStringTemplateEntries(0)?.getSingleEntry() ?: return
            if (dependencyText != apollo3) return
            when (val element = callExpression.parent) {
              is KtBinaryExpression -> {
                // id("xxx") version yyy
                val version = (element.right as? KtStringTemplateExpression)?.entries?.getSingleEntry() ?: return
                if (!version.startsWith("4")) {
                  holder.registerProblem(element, ApolloBundle.message("inspection.apollo4Available.reportText"), Apollo4AvailableQuickFix)
                }
              }

              is KtDotQualifiedExpression -> {
                // id("xxx").version(yyy)
                val versionCallExpression = element.selectorExpression as? KtCallExpression
                val version = versionCallExpression?.getArgumentAsStringTemplateEntries(0)?.getSingleEntry() ?: return
                if (!version.startsWith("4")) {
                  holder.registerProblem(element, ApolloBundle.message("inspection.apollo4Available.reportText"), Apollo4AvailableQuickFix)
                }
              }
            }
          }

          "implementation", "api", "testImplementation", "testApi" -> {
            when (callExpression.valueArguments.size) {
              // implementation("xxx:yyy:zzz")
              1 -> {
                val dependency = callExpression.getArgumentAsStringTemplateEntries(0)?.getSingleEntry() ?: return
                val dependencyElements = dependency.split(":")
                if (dependencyElements.size != 3) return
                val groupId = dependencyElements[0]
                if (groupId != apollo3) return
                val version = dependencyElements[2]
                if (!version.startsWith("4")) {
                  holder.registerProblem(callExpression, ApolloBundle.message("inspection.apollo4Available.reportText"), Apollo4AvailableQuickFix)
                }
              }


              // implementation("xxx", "yyy", "zzz")
              3 -> {
                val groupId = callExpression.getArgumentAsStringTemplateEntries(0)?.getSingleEntry() ?: return
                if (groupId != apollo3) return
                val version = callExpression.getArgumentAsStringTemplateEntries(2)?.getSingleEntry() ?: return
                if (!version.startsWith("4")) {
                  holder.registerProblem(callExpression, ApolloBundle.message("inspection.apollo4Available.reportText"), Apollo4AvailableQuickFix)
                }
              }
            }
          }
        }
      }

      private fun KtCallExpression.getArgumentAsStringTemplateEntries(index: Int): Array<KtStringTemplateEntry>? =
          (valueArgumentList?.arguments?.getOrNull(index)
              ?.children?.firstOrNull() as? KtStringTemplateExpression)?.entries

      // Only consider simple strings (no templates)
      private fun Array<KtStringTemplateEntry>.getSingleEntry(): String? {
        if (size != 1 || this[0] !is KtLiteralStringTemplateEntry) return null
        return this[0].text.unquoted()
      }
    }
  }
}

object Apollo4AvailableQuickFix : LocalQuickFix {
  override fun getName() = ApolloBundle.message("inspection.apollo4Available.quickFix")

  override fun getFamilyName() = name

  override fun availableInBatchMode() = false

  override fun generatePreview(project: Project, previewDescriptor: ProblemDescriptor): IntentionPreviewInfo = IntentionPreviewInfo.EMPTY

  override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
    if (!IntentionPreviewUtils.isIntentionPreviewActive()) project.telemetryService.logEvent(TelemetryEvent.ApolloIjApollo4AvailableQuickFix())
    val action = ActionManager.getInstance().getAction(ApolloV3ToV4MigrationAction.ACTION_ID)
    ActionManager.getInstance().tryToExecute(action, null, null, null, false)
  }
}
