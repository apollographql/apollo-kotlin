package com.apollographql.ijplugin.inspection

import com.apollographql.ijplugin.ApolloBundle
import com.apollographql.ijplugin.project.apolloProjectService
import com.apollographql.ijplugin.telemetry.TelemetryEvent
import com.apollographql.ijplugin.telemetry.telemetryService
import com.apollographql.ijplugin.util.getMethodName
import com.apollographql.ijplugin.util.isMethodCall
import com.apollographql.ijplugin.util.lambdaBlockExpression
import com.intellij.codeInsight.intention.preview.IntentionPreviewUtils
import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.util.findParentInFile
import org.jetbrains.kotlin.idea.util.application.isUnitTestMode
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.psi.KtVisitorVoid

class ApolloEndpointNotConfiguredInspection : LocalInspectionTool() {
  // XXX kts files are not highlighted in tests
  private val buildGradleFileName = if (isUnitTestMode()) "build.gradle.kt" else "build.gradle.kts"

  override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
    return object : KtVisitorVoid() {
      override fun visitCallExpression(expression: KtCallExpression) {
        super.visitCallExpression(expression)
        if (!expression.project.apolloProjectService.apolloVersion.isAtLeastV3) return
        if (expression.containingFile.name != buildGradleFileName) return
        if (expression.getMethodName() == "service" && expression.findParentInFile { it.isMethodCall("apollo") } != null) {
          val serviceBlockExpression = expression.lambdaBlockExpression() ?: return
          // Don't suggest to add an introspection block if this is a submodule
          val hasDependsOn = serviceBlockExpression.statements.any { it.isMethodCall("dependsOn") }
          if (hasDependsOn) return
          if (serviceBlockExpression.statements.none { it.isMethodCall("introspection") }) {
            holder.registerProblem(expression.calleeExpression!!, ApolloBundle.message("inspection.endpointNotConfigured.reportText"), AddIntrospectionBlockQuickFix)
          }
        }
      }
    }
  }
}

object AddIntrospectionBlockQuickFix : LocalQuickFix {
  override fun getName() = ApolloBundle.message("inspection.endpointNotConfigured.quickFix")
  override fun getFamilyName() = name

  override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
    if (!IntentionPreviewUtils.isIntentionPreviewActive()) project.telemetryService.logEvent(TelemetryEvent.ApolloIjEndpointNotConfiguredQuickFix())
    val callExpression = descriptor.psiElement.parent as KtCallExpression
    val serviceBlockExpression = callExpression.lambdaBlockExpression() ?: return
    val ktFactory = KtPsiFactory(project)
    val newCallExpression = ktFactory.createExpression(
        """
        introspection {
            endpointUrl.set("https://example.com/graphql")
            headers.put("api-key", "1234567890abcdef")
            schemaFile.set(file("src/main/graphql/schema.graphqls"))
        }
      """.trimIndent()
    )
    serviceBlockExpression.add(ktFactory.createNewLine())
    serviceBlockExpression.add(newCallExpression)
  }
}
