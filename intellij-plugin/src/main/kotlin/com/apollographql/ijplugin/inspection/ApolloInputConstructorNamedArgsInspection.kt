package com.apollographql.ijplugin.inspection

import com.apollographql.ijplugin.ApolloBundle
import com.apollographql.ijplugin.navigation.isApolloInputClassReference
import com.apollographql.ijplugin.project.apolloProjectService
import com.apollographql.ijplugin.telemetry.TelemetryEvent
import com.apollographql.ijplugin.telemetry.telemetryService
import com.apollographql.ijplugin.util.cast
import com.apollographql.ijplugin.util.originalClassName
import com.apollographql.ijplugin.util.registerProblem
import com.intellij.codeInsight.intention.preview.IntentionPreviewUtils
import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElementVisitor
import org.jetbrains.kotlin.idea.base.psi.kotlinFqName
import org.jetbrains.kotlin.idea.caches.resolve.resolveToCall
import org.jetbrains.kotlin.idea.references.mainReference
import org.jetbrains.kotlin.psi.KtCallElement
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression
import org.jetbrains.kotlin.psi.KtNameReferenceExpression
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.psi.KtValueArgument
import org.jetbrains.kotlin.psi.KtVisitorVoid
import org.jetbrains.kotlin.resolve.calls.model.ArgumentMatch

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
          add(AddArgumentNamesQuickFix)
          if (projectApolloVersion.isAtLeastV4) add(ChangeToBuilderQuickFix)
        }.toTypedArray()
        holder.registerProblem(expression, ApolloBundle.message("inspection.inputConstructorNamedArgs.reportText"), withMoreLink = true, *quickFixes)
      }
    }
  }
}

object AddArgumentNamesQuickFix : LocalQuickFix {
  override fun getName() = ApolloBundle.message("inspection.inputConstructorNamedArgs.quickFix.addArgumentNames")
  override fun getFamilyName() = name

  override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
    if (!IntentionPreviewUtils.isIntentionPreviewActive()) project.telemetryService.logEvent(TelemetryEvent.ApolloIjInputConstructorNamedArgsAddArgumentNamesQuickFix())
    val callElement = descriptor.psiElement as KtCallElement
    val arguments = callElement.valueArguments
    val resolvedCall = callElement.resolveToCall() ?: return
    for (argument in arguments) {
      val ktArgument = argument.cast<KtValueArgument>() ?: continue
      val name = (resolvedCall.getArgumentMapping(argument) as? ArgumentMatch)?.valueParameter?.name ?: continue
      ktArgument.replace(KtPsiFactory(project).createArgument("$name = ${ktArgument.getArgumentExpression()!!.text}"))
    }
  }
}

object ChangeToBuilderQuickFix : LocalQuickFix {
  override fun getName() = ApolloBundle.message("inspection.inputConstructorNamedArgs.quickFix.changeToBuilder")
  override fun getFamilyName() = name

  override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
    if (!IntentionPreviewUtils.isIntentionPreviewActive()) project.telemetryService.logEvent(TelemetryEvent.ApolloIjInputConstructorNamedArgsChangeToBuilderQuickFix())
    val callElement = descriptor.psiElement as KtCallElement
    applyFix(project, callElement)
  }

  fun applyFix(project: Project, callElement: KtCallElement) {
    val arguments = callElement.valueArguments
    val resolvedCall = callElement.resolveToCall() ?: return
    val inputClassName = callElement.calleeExpression.cast<KtNameReferenceExpression>()?.originalClassName() ?: return
    val replacementExpression = buildString {
      append("$inputClassName.Builder()")
      for (argument in arguments) {
        val ktArgument = argument.cast<KtValueArgument>() ?: continue
        val name = (resolvedCall.getArgumentMapping(argument) as? ArgumentMatch)?.valueParameter?.name ?: continue
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
    val isOptional = receiverClassName == "com.apollographql.apollo3.api.Optional"
    val isOptionalCompanion = receiverClassName == "com.apollographql.apollo3.api.Optional.Companion"
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
