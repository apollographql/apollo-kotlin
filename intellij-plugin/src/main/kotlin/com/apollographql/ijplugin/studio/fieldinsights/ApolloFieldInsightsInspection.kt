package com.apollographql.ijplugin.studio.fieldinsights

import com.apollographql.ijplugin.ApolloBundle
import com.apollographql.ijplugin.gradle.ApolloKotlinService
import com.apollographql.ijplugin.telemetry.TelemetryEvent
import com.apollographql.ijplugin.telemetry.telemetryService
import com.apollographql.ijplugin.util.findChildrenOfType
import com.intellij.codeInsight.intention.preview.IntentionPreviewUtils
import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.codeInspection.ui.SingleIntegerFieldOptionsPanel
import com.intellij.lang.jsgraphql.ide.config.GraphQLConfigProvider
import com.intellij.lang.jsgraphql.ide.config.model.GraphQLProjectConfig
import com.intellij.lang.jsgraphql.psi.GraphQLElement
import com.intellij.lang.jsgraphql.psi.GraphQLElementFactory
import com.intellij.lang.jsgraphql.psi.GraphQLField
import com.intellij.lang.jsgraphql.psi.GraphQLFieldDefinition
import com.intellij.lang.jsgraphql.psi.GraphQLIdentifier
import com.intellij.lang.jsgraphql.psi.GraphQLInlineFragment
import com.intellij.lang.jsgraphql.psi.GraphQLSelection
import com.intellij.lang.jsgraphql.psi.GraphQLSelectionSetOperationDefinition
import com.intellij.lang.jsgraphql.psi.GraphQLTypeDefinition
import com.intellij.lang.jsgraphql.psi.GraphQLTypeNameDefinition
import com.intellij.lang.jsgraphql.psi.GraphQLVisitor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.PsiFile
import com.intellij.psi.codeStyle.CodeStyleManager
import com.intellij.psi.util.findParentOfType
import org.jetbrains.kotlin.idea.util.application.isUnitTestMode
import kotlin.math.roundToInt

class ApolloFieldInsightsInspection : LocalInspectionTool() {
  @JvmField
  var thresholdMs = 100

  override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
    return object : GraphQLVisitor() {
      override fun visitIdentifier(o: GraphQLIdentifier) {
        if (!o.project.fieldInsightsService.hasLatencies()) return
        val latency = getLatency(o) ?: return
        val field = o.parent as? GraphQLField ?: return
        if (field.isDeferred()) return
        reportIfHighLatency(o, field.name ?: "", latency, withQuickFix = true)
      }

      override fun visitFieldDefinition(o: GraphQLFieldDefinition) {
        if (!o.project.fieldInsightsService.hasLatencies()) return
        val latency = getLatency(o) ?: return
        reportIfHighLatency(o, o.name ?: "", latency, withQuickFix = false)
      }

      private fun reportIfHighLatency(element: GraphQLElement, fieldName: String, latency: Double, withQuickFix: Boolean) {
        if (latency < thresholdMs) return
        val message = ApolloBundle.message("inspection.fieldInsights.reportText", fieldName, latency.toFormattedString())
        if (withQuickFix) {
          holder.registerProblem(element, message, EncloseInDeferredFragmentQuickFix())
        } else {
          holder.registerProblem(element, message)
        }
      }
    }
  }

  private fun getLatency(identifier: GraphQLIdentifier): Double? {
    val field = identifier.parent as? GraphQLField ?: return null
    return getLatency(field)
  }

  private fun getLatency(field: GraphQLField): Double? {
    val fieldDefinition = field.findChildrenOfType<GraphQLIdentifier>().firstOrNull()?.reference?.resolve()?.parent as? GraphQLFieldDefinition
        ?: return null
    return getLatency(fieldDefinition)
  }

  private fun getLatency(fieldDefinition: GraphQLFieldDefinition): Double? {
    val typeDefinition = fieldDefinition.parent.parent as? GraphQLTypeDefinition ?: return null
    val typeName = typeDefinition.findChildrenOfType<GraphQLTypeNameDefinition>().firstOrNull()?.name ?: return null
    val fieldName = fieldDefinition.name ?: return null
    val serviceId = fieldDefinition.containingFile.getApolloKotlinServiceId() ?: return null
    return fieldDefinition.project.fieldInsightsService.getLatency(serviceId, typeName, fieldName)
  }

  private fun GraphQLField.isDeferred(): Boolean {
    return findParentOfType<GraphQLInlineFragment>()?.directives?.any { it.name == "defer" } == true
  }

  private fun PsiFile.getApolloKotlinServiceId(): ApolloKotlinService.Id? {
    if (isUnitTestMode()) return ApolloKotlinService.Id("dummy", "dummy")
    val config: GraphQLProjectConfig = GraphQLConfigProvider.getInstance(project).resolveProjectConfig(this)
        ?: return null
    return ApolloKotlinService.Id.fromString(config.name)
  }

  private fun Double.toFormattedString() = "~${roundToInt()} ms"

  override fun createOptionsPanel() = SingleIntegerFieldOptionsPanel(ApolloBundle.message("inspection.fieldInsights.settings.threshold"), this, "thresholdMs")

  private class EncloseInDeferredFragmentQuickFix : LocalQuickFix {
    override fun getName() = ApolloBundle.message("inspection.fieldInsights.quickFix")
    override fun getFamilyName() = name

    override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
      if (!IntentionPreviewUtils.isIntentionPreviewActive()) project.telemetryService.logEvent(TelemetryEvent.ApolloIjHighLatencyFieldQuickFix())
      val resolvedIdentifier = descriptor.psiElement.reference?.resolve() as? GraphQLIdentifier ?: return
      val typeDefinition = resolvedIdentifier.findParentOfType<GraphQLTypeDefinition>() ?: return
      val typeName = typeDefinition.findChildrenOfType<GraphQLTypeNameDefinition>().firstOrNull()?.name ?: return

      val originalSelection = descriptor.psiElement.findParentOfType<GraphQLSelection>() ?: return

      val graphQLFile = GraphQLElementFactory.createFile(project, "{ ... on $typeName @defer {\n${originalSelection.text}\n} }")
      val newSelection = (graphQLFile.firstChild as GraphQLSelectionSetOperationDefinition).selectionSet.selectionList[0] as GraphQLSelection

      originalSelection.replace(newSelection)

      CodeStyleManager.getInstance(project).reformat(newSelection.parent)
    }
  }
}
