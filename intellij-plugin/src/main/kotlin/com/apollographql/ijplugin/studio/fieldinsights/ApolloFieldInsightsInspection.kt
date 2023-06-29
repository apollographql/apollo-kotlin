package com.apollographql.ijplugin.studio.fieldinsights

import com.apollographql.ijplugin.ApolloBundle
import com.apollographql.ijplugin.gradle.ApolloKotlinService
import com.apollographql.ijplugin.util.findChildrenOfType
import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.codeInspection.ui.SingleIntegerFieldOptionsPanel
import com.intellij.lang.jsgraphql.ide.config.GraphQLConfigProvider
import com.intellij.lang.jsgraphql.ide.config.model.GraphQLProjectConfig
import com.intellij.lang.jsgraphql.psi.GraphQLElement
import com.intellij.lang.jsgraphql.psi.GraphQLField
import com.intellij.lang.jsgraphql.psi.GraphQLFieldDefinition
import com.intellij.lang.jsgraphql.psi.GraphQLIdentifier
import com.intellij.lang.jsgraphql.psi.GraphQLTypeDefinition
import com.intellij.lang.jsgraphql.psi.GraphQLTypeNameDefinition
import com.intellij.lang.jsgraphql.psi.GraphQLVisitor
import com.intellij.openapi.components.service
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.PsiFile
import kotlin.math.roundToInt

class ApolloFieldInsightsInspection : LocalInspectionTool() {
  @JvmField
  var thresholdMs = 100

  override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
    return object : GraphQLVisitor() {
      override fun visitIdentifier(o: GraphQLIdentifier) {
        if (!o.project.service<FieldInsightsService>().hasLatencies()) return
        val latency = getLatency(o) ?: return
        val field = o.parent as? GraphQLField ?: return
        reportIfHighLatency(o, field.name ?: "", latency)
      }

      override fun visitFieldDefinition(o: GraphQLFieldDefinition) {
        if (!o.project.service<FieldInsightsService>().hasLatencies()) return
        val latency = getLatency(o) ?: return
        reportIfHighLatency(o, o.name ?: "", latency)
      }

      private fun reportIfHighLatency(element: GraphQLElement, fieldName: String, latency: Double) {
        if (latency < thresholdMs) return
        holder.registerProblem(element, ApolloBundle.message("inspection.fieldInsights.reportText", fieldName, latency.toFormattedString()))
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
    return fieldDefinition.project.service<FieldInsightsService>().getLatency(serviceId, typeName, fieldName)
  }

  private fun PsiFile.getApolloKotlinServiceId(): ApolloKotlinService.Id? {
    val config: GraphQLProjectConfig = GraphQLConfigProvider.getInstance(project).resolveProjectConfig(this)
        ?: return null
    return ApolloKotlinService.Id.fromString(config.name)
  }

  private fun Double.toFormattedString() = "~${roundToInt()} ms"

  override fun createOptionsPanel() = SingleIntegerFieldOptionsPanel(ApolloBundle.message("inspection.fieldInsights.settings.threshold"), this, "thresholdMs")
}
