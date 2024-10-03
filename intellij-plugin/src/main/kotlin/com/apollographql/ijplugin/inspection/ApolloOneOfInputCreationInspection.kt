package com.apollographql.ijplugin.inspection

import com.apollographql.ijplugin.ApolloBundle
import com.apollographql.ijplugin.navigation.findInputTypeGraphQLDefinitions
import com.apollographql.ijplugin.navigation.isApolloInputClass
import com.apollographql.ijplugin.navigation.isApolloInputClassReference
import com.apollographql.ijplugin.project.apolloProjectService
import com.apollographql.ijplugin.util.apollo4
import com.apollographql.ijplugin.util.canBeNull
import com.apollographql.ijplugin.util.cast
import com.apollographql.ijplugin.util.className
import com.apollographql.ijplugin.util.getCalleeExpressionIfAny
import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.lang.jsgraphql.psi.GraphQLInputObjectTypeDefinition
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.util.findTopmostParentOfType
import com.intellij.psi.util.parentOfType
import org.jetbrains.kotlin.idea.intentions.branchedTransformations.isNullExpression
import org.jetbrains.kotlin.idea.references.mainReference
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtNameReferenceExpression
import org.jetbrains.kotlin.psi.KtVisitorVoid
import org.jetbrains.kotlin.psi.psiUtil.containingClass

class ApolloOneOfInputCreationInspection : LocalInspectionTool() {
  override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
    return object : KtVisitorVoid() {
      private val alreadyVisitedBuildExpressions = mutableSetOf<KtExpression>()

      // For constructor calls
      override fun visitCallExpression(expression: KtCallExpression) {
        super.visitCallExpression(expression)
        if (!expression.project.apolloProjectService.apolloVersion.isAtLeastV4) return
        val reference = (expression.calleeExpression.cast<KtNameReferenceExpression>())
        if (reference?.isApolloInputClassReference() != true) return
        val inputTypeName = reference.text
        val inputTypeDefinition = findInputTypeGraphQLDefinitions(expression.project, inputTypeName).firstOrNull()
            ?.parentOfType<GraphQLInputObjectTypeDefinition>()
            ?: return
        val isOneOf = inputTypeDefinition.directives.any { it.name == "oneOf" }
        if (!isOneOf) return
        if (expression.valueArguments.size != 1) {
          holder.registerProblem(expression.calleeExpression!!, ApolloBundle.message("inspection.oneOfInputCreation.reportText.constructor.wrongNumberOfArgs"))
          return
        }
        val arg = expression.valueArguments.first()
        if (arg.getArgumentExpression()?.className() == "$apollo4.api.Optional.Absent") {
          holder.registerProblem(expression.calleeExpression!!, ApolloBundle.message("inspection.oneOfInputCreation.reportText.constructor.argIsAbsent"))
        }
      }

      // For builder calls
      override fun visitDotQualifiedExpression(expression: KtDotQualifiedExpression) {
        super.visitDotQualifiedExpression(expression)
        if (!expression.project.apolloProjectService.apolloVersion.isAtLeastV4) return
        val expressionClass =
          expression.selectorExpression.getCalleeExpressionIfAny()?.mainReference?.resolve()?.parentOfType<KtClass>(withSelf = true)
              ?: return
        if (expressionClass.name != "Builder") return
        val containingClass = expressionClass.containingClass() ?: return
        if (!containingClass.isApolloInputClass()) return
        val inputTypeName = containingClass.name ?: return
        val inputTypeDefinition = findInputTypeGraphQLDefinitions(expression.project, inputTypeName).firstOrNull()
            ?.parentOfType<GraphQLInputObjectTypeDefinition>()
            ?: return
        val isOneOf = inputTypeDefinition.directives.any { it.name == "oneOf" }
        if (!isOneOf) return
        val arguments = expression.selectorExpression.cast<KtCallExpression>()?.valueArguments ?: return
        // No arguments: we're on the MyInput.Builder() or .build() call
        if (arguments.size == 0) return

        val argumentExpression = arguments.first().getArgumentExpression()
        if (argumentExpression?.isNullExpression() == true) {
          // `null`
          holder.registerProblem(expression.selectorExpression!!, ApolloBundle.message("inspection.oneOfInputCreation.reportText.builder.argIsNull"))
        } else if (argumentExpression?.canBeNull() == true) {
          // a nullable type: warning only
          holder.registerProblem(expression.selectorExpression!!, ApolloBundle.message("inspection.oneOfInputCreation.reportText.builder.argIsNull"), ProblemHighlightType.WARNING)
        }

        val buildExpression = expression.findTopmostParentOfType<KtDotQualifiedExpression>() ?: return
        // If we've already visited the build expression it means several fields are being set in the same builder
        if (buildExpression in alreadyVisitedBuildExpressions) {
          holder.registerProblem(expression.selectorExpression!!, ApolloBundle.message("inspection.oneOfInputCreation.reportText.builder.wrongNumberOfArgs"))
        }
        alreadyVisitedBuildExpressions.add(buildExpression)
      }
    }
  }
}
