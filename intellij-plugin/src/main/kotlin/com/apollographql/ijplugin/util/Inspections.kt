package com.apollographql.ijplugin.util

import com.apollographql.ijplugin.ApolloBundle
import com.intellij.codeInsight.daemon.impl.analysis.DaemonTooltipsUtil
import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptorBase
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiElement

fun ProblemsHolder.registerProblem(
    localInspectionTool: LocalInspectionTool,
    element: PsiElement,
    description: String,
    withMoreLink: Boolean,
    vararg fixes: LocalQuickFix,
) {
  registerProblem(localInspectionTool, element, description, ProblemHighlightType.GENERIC_ERROR_OR_WARNING, withMoreLink, *fixes)
}

fun ProblemsHolder.registerProblem(
    localInspectionTool: LocalInspectionTool,
    element: PsiElement,
    description: String,
    highlightType: ProblemHighlightType,
    withMoreLink: Boolean,
    vararg fixes: LocalQuickFix,
) {
  if (!withMoreLink) {
    registerProblem(element, description, highlightType, *fixes)
    return
  }
  registerProblem(
      object : ProblemDescriptorBase(
          element,
          element,
          description,
          fixes,
          highlightType,
          false,
          null,
          true,
          isOnTheFly
      ) {
        override fun getTooltipTemplate(): String {
          return "<html>$description <a href=\"#inspection/${localInspectionTool.shortName}\">${ApolloBundle.message("inspection.more")}</a> ${DaemonTooltipsUtil.getShortcutText()}"
        }
      }
  )
}
