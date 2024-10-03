package com.apollographql.ijplugin.intention

import com.apollographql.ijplugin.ApolloBundle
import com.apollographql.ijplugin.inspection.ChangeToBuilderQuickFix
import com.apollographql.ijplugin.navigation.isApolloInputClassReference
import com.apollographql.ijplugin.project.apolloProjectService
import com.apollographql.ijplugin.telemetry.TelemetryEvent
import com.apollographql.ijplugin.telemetry.telemetryService
import com.apollographql.ijplugin.util.cast
import com.apollographql.ijplugin.util.getParameterNames
import com.intellij.codeInsight.intention.PsiElementBaseIntentionAction
import com.intellij.codeInsight.intention.preview.IntentionPreviewUtils
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.impl.source.tree.LeafPsiElement
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtNameReferenceExpression

class ApolloInputConstructorChangeToBuilderIntention : PsiElementBaseIntentionAction() {
  override fun getText() = ApolloBundle.message("intention.InputConstructorChangeToBuilder.name.editor")
  override fun getFamilyName() = ApolloBundle.message("intention.InputConstructorChangeToBuilder.name.settings")

  override fun isAvailable(project: Project, editor: Editor?, element: PsiElement): Boolean {
    if (!element.project.apolloProjectService.apolloVersion.isAtLeastV4) return false
    val callExpression = (element as? LeafPsiElement)?.parent?.parent as? KtCallExpression ?: return false
    val reference = callExpression.calleeExpression.cast<KtNameReferenceExpression>() ?: return false
    return reference.isApolloInputClassReference()
  }

  override fun invoke(project: Project, editor: Editor?, element: PsiElement) {
    if (!IntentionPreviewUtils.isIntentionPreviewActive()) project.telemetryService.logEvent(TelemetryEvent.ApolloIjInputConstructorChangeToBuilderIntentionApply())
    val callExpression = element.parent.parent as KtCallExpression
    val parameterNames = callExpression.getParameterNames() ?: return
    ChangeToBuilderQuickFix(parameterNames).applyFix(project, callExpression)
  }
}
