package com.apollographql.ijplugin.inspection

import com.apollographql.ijplugin.ApolloBundle
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
  private val quickFix = RenameToGraphqlsQuickFix()

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
        if (o.containingFile.name.endsWith(".graphql")) {
          holder.registerProblem(o, ApolloBundle.message("inspection.schemaInGraphqlFile.reportText"), quickFix)
        }
      }
    }
  }

  private class RenameToGraphqlsQuickFix : LocalQuickFix {
    override fun getName() = ApolloBundle.message("inspection.schemaInGraphqlFile.quickFix")
    override fun getFamilyName() = name

    override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
      val psiFile = descriptor.psiElement.containingFile
      val newName = psiFile.name.replace(".graphql", ".graphqls")
      psiFile.virtualFile.rename(this, newName)
    }
  }
}
