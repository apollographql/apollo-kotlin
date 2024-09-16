package com.apollographql.ijplugin.inspection

import com.apollographql.ijplugin.ApolloBundle
import com.apollographql.ijplugin.navigation.isApolloInputClassReference
import com.apollographql.ijplugin.project.apolloProjectService
import com.apollographql.ijplugin.telemetry.TelemetryEvent
import com.apollographql.ijplugin.telemetry.telemetryService
import com.apollographql.ijplugin.util.apollo3
import com.apollographql.ijplugin.util.apollo4
import com.apollographql.ijplugin.util.cast
import com.apollographql.ijplugin.util.getParameterNames
import com.apollographql.ijplugin.util.originalClassName
import com.apollographql.ijplugin.util.registerProblem
import com.intellij.codeInsight.intention.FileModifier
import com.intellij.codeInsight.intention.preview.IntentionPreviewUtils
import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElementVisitor
import org.jetbrains.kotlin.idea.base.psi.kotlinFqName
import org.jetbrains.kotlin.idea.references.mainReference
import org.jetbrains.kotlin.psi.KtCallElement
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression
import org.jetbrains.kotlin.psi.KtNameReferenceExpression
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.psi.KtValueArgument
import org.jetbrains.kotlin.psi.KtVisitorVoid

class ApolloInputConstructorNamedArgsInspection : LocalInspectionTool() {
  override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
    return object : KtVisitorVoid() {
      override fun visitCallExpression(expression: KtCallExpression) {
        super.visitCallExpression(expression)
        val projectApolloVersion = expression.project.apolloProjectService.apolloVersion
        if (!projectApolloVersion.isAtLeastV3) return
        val reference = expression.calleeExpression.cast<KtNameReferenceExpression>()
        if (expression.valueArguments.size < 2) return
        if (expression.valueArguments.all { it.isNamed() }) return
        if (reference?.isApolloInputClassReference() != true) return
        val quickFixes = buildList {
          val parameterNames = expression.getParameterNames() ?: return@buildList
          add(AddArgumentNamesQuickFix(parameterNames))
          if (projectApolloVersion.isAtLeastV4) add(ChangeToBuilderQuickFix(parameterNames))
        }.toTypedArray()
        holder.registerProblem(this@ApolloInputConstructorNamedArgsInspection, expression, ApolloBundle.message("inspection.inputConstructorNamedArgs.reportText"), withMoreLink = true, *quickFixes)
      }
    }
  }
}

class AddArgumentNamesQuickFix(
    @FileModifier.SafeFieldForPreview
    private val parameterNames: List<String>,
) : LocalQuickFix {
  override fun getName() = ApolloBundle.message("inspection.inputConstructorNamedArgs.quickFix.addArgumentNames")
  override fun getFamilyName() = name

  override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
    if (!IntentionPreviewUtils.isIntentionPreviewActive()) project.telemetryService.logEvent(TelemetryEvent.ApolloIjInputConstructorNamedArgsAddArgumentNamesQuickFix())
    val callElement = descriptor.psiElement as KtCallElement
    val arguments = callElement.valueArguments
    for ((i, argument) in arguments.withIndex()) {
      val ktArgument = argument.cast<KtValueArgument>() ?: continue
      ktArgument.replace(KtPsiFactory(project).createArgument("${parameterNames[i]} = ${ktArgument.getArgumentExpression()!!.text}"))
    }
  }
}

class ChangeToBuilderQuickFix(
    @FileModifier.SafeFieldForPreview
    private val parameterNames: List<String>,
) : LocalQuickFix {
  override fun getName() = ApolloBundle.message("inspection.inputConstructorNamedArgs.quickFix.changeToBuilder")
  override fun getFamilyName() = name

  override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
    if (!IntentionPreviewUtils.isIntentionPreviewActive()) project.telemetryService.logEvent(TelemetryEvent.ApolloIjInputConstructorNamedArgsChangeToBuilderQuickFix())
    val callElement = descriptor.psiElement as KtCallElement
    applyFix(project, callElement)
  }

  fun applyFix(project: Project, callElement: KtCallElement) {
    val arguments = callElement.valueArguments
    val inputClassName = callElement.calleeExpression.cast<KtNameReferenceExpression>()?.originalClassName() ?: return
    val replacementExpression = buildString {
      append("$inputClassName.Builder()")
      for ((i, argument) in arguments.withIndex()) {
        val ktArgument = argument.cast<KtValueArgument>() ?: continue
        val name = parameterNames[i]
        val argumentText = ktArgument.unwrapOptional()
        if (argumentText != null) {
          append("\n.$name($argumentText)")
        }
      }
      append("\n.build()")
    }
    callElement.replace(KtPsiFactory(project).createExpression(replacementExpression))
  }

  /**
   * - Optional.present(xxx) -> `"xxx"`
   * - Optional.Present(xxx) -> `"xxx"`
   * - Optional.absent() -> `null`
   * - Optional.Absent -> `null`
   * - xxx -> `"xxx.getOrNull()"`
   */
  private fun KtValueArgument.unwrapOptional(): String? {
    val argumentExpression = getArgumentExpression() ?: return "?"
    val dotQualifiedExpression = argumentExpression.cast<KtDotQualifiedExpression>()
    val receiverClassName = dotQualifiedExpression?.receiverExpression?.mainReference?.resolve()?.kotlinFqName?.asString()
    val isOptional = receiverClassName == "$apollo3.api.Optional" || receiverClassName == "$apollo4.api.Optional"
    val isOptionalCompanion =
      receiverClassName == "$apollo3.api.Optional.Companion" || receiverClassName == "$apollo4.api.Optional.Companion"
    val selectorCallExpression = dotQualifiedExpression?.selectorExpression.cast<KtCallExpression>()
    val selectorCallExpressionText = selectorCallExpression?.calleeExpression?.text
    val nameReferenceExpressionText = dotQualifiedExpression?.selectorExpression.cast<KtNameReferenceExpression>()?.text
    val isPresent = isOptional && selectorCallExpressionText == "Present" || isOptionalCompanion && selectorCallExpressionText == "present"
    val presentArgumentText = selectorCallExpression?.valueArguments?.firstOrNull()?.text ?: "?"
    val isAbsent = isOptional && nameReferenceExpressionText == "Absent" || isOptionalCompanion && selectorCallExpressionText == "absent"
    return when {
      isPresent -> presentArgumentText
      isAbsent -> null
      else -> "${argumentExpression.text}.getOrNull()"
    }
  }
}
