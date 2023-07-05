package com.apollographql.ijplugin.inspection

import com.apollographql.ijplugin.ApolloBundle
import com.intellij.codeInsight.intention.FileModifier.SafeFieldForPreview
import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement

class DeleteElementQuickFix(
    private val label: String,

    @SafeFieldForPreview
    private val elementToDelete: (PsiElement) -> PsiElement,
) : LocalQuickFix {
  override fun getName() = ApolloBundle.message(label)

  override fun getFamilyName() = name

  override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
    elementToDelete(descriptor.psiElement).delete()
  }
}
