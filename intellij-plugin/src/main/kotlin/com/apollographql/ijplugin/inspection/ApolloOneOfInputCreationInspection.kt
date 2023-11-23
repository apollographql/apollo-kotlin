package com.apollographql.ijplugin.inspection

import com.apollographql.ijplugin.ApolloBundle
import com.apollographql.ijplugin.navigation.findInputTypeGraphQLDefinitions
import com.apollographql.ijplugin.navigation.isApolloInputClassReference
import com.apollographql.ijplugin.project.apolloProjectService
import com.apollographql.ijplugin.util.cast
import com.apollographql.ijplugin.util.type
import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.lang.jsgraphql.psi.GraphQLInputObjectTypeDefinition
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.util.parentOfType
import org.jetbrains.kotlin.idea.base.utils.fqname.fqName
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtNameReferenceExpression
import org.jetbrains.kotlin.psi.KtVisitorVoid

class ApolloOneOfInputCreationInspection : LocalInspectionTool() {
  override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
    return object : KtVisitorVoid() {
      override fun visitCallExpression(expression: KtCallExpression) {
        super.visitCallExpression(expression)
        if (!expression.project.apolloProjectService.apolloVersion.isAtLeastV4) return
        val reference = (expression.calleeExpression.cast<KtNameReferenceExpression>())
        if (reference?.isApolloInputClassReference() != true) return
        val inputTypeName = reference.text
        val inputTypeDefinition = findInputTypeGraphQLDefinitions(reference.project, inputTypeName).firstOrNull()
            ?.parentOfType<GraphQLInputObjectTypeDefinition>()
            ?: return
        val isOneOf = inputTypeDefinition.directives.any { it.name == "oneOf" }
        if (!isOneOf) return
        if (expression.valueArguments.size != 1) {
          holder.registerProblem(expression.calleeExpression!!, ApolloBundle.message("inspection.oneOfInputCreation.reportText.wrongNumberOfArgs"))
          return
        }
        val arg = expression.valueArguments.first()
        if (arg.getArgumentExpression()?.type()?.fqName?.asString() == "com.apollographql.apollo3.api.Optional.Absent") {
          holder.registerProblem(expression.calleeExpression!!, ApolloBundle.message("inspection.oneOfInputCreation.reportText.argIsAbsent"))
        }
      }
    }
  }
}
