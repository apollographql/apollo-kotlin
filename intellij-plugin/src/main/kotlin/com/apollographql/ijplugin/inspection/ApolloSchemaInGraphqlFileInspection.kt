package com.apollographql.ijplugin.inspection

import com.apollographql.ijplugin.ApolloBundle
import com.apollographql.ijplugin.telemetry.TelemetryEvent
import com.apollographql.ijplugin.telemetry.telemetryService
import com.intellij.codeInsight.intention.preview.IntentionPreviewInfo
import com.intellij.codeInsight.intention.preview.IntentionPreviewUtils
import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.lang.jsgraphql.psi.GraphQLDirectiveDefinition
import com.intellij.lang.jsgraphql.psi.GraphQLElement
import com.intellij.lang.jsgraphql.psi.GraphQLSchemaDefinition
import com.intellij.lang.jsgraphql.psi.GraphQLTypeDefinition
import com.intellij.lang.jsgraphql.psi.GraphQLVisitor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElementVisitor

class ApolloSchemaInGraphqlFileInspection : LocalInspectionTool() {
  override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
    return object : GraphQLVisitor() {
      override fun visitSchemaDefinition(o: GraphQLSchemaDefinition) {
        checkForInvalidFile(o)
      }

      override fun visitTypeDefinition(o: GraphQLTypeDefinition) {
        checkForInvalidFile(o)
      }

      override fun visitDirectiveDefinition(o: GraphQLDirectiveDefinition) {
        checkForInvalidFile(o)
      }

      private fun checkForInvalidFile(o: GraphQLElement) {
        val currentFileName = o.containingFile.name
        if (currentFileName.endsWith(".graphql")) {
          holder.registerProblem(o, ApolloBundle.message("inspection.schemaInGraphqlFile.reportText"), RenameToGraphqlsQuickFix(currentFileName))
        }
      }
    }
  }

  private class RenameToGraphqlsQuickFix(private val currentFileName: String) : LocalQuickFix {
    override fun getName(): String {
      val newFileName = currentFileName.replace(".graphql", ".graphqls")
      return ApolloBundle.message("inspection.schemaInGraphqlFile.quickFix", newFileName)
    }
    override fun getFamilyName() = name

    override fun generatePreview(project: Project, previewDescriptor: ProblemDescriptor): IntentionPreviewInfo = IntentionPreviewInfo.EMPTY

    override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
      if (!IntentionPreviewUtils.isIntentionPreviewActive()) project.telemetryService.logEvent(TelemetryEvent.ApolloIjSchemaInGraphqlFileQuickFix())
      val psiFile = descriptor.psiElement.containingFile
      val newName = psiFile.name.replace(".graphql", ".graphqls")
      psiFile.virtualFile.rename(this, newName)
    }
  }
}
