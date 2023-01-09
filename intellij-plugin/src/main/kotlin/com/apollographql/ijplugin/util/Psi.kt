package com.apollographql.ijplugin.util

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtImportList
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType

fun PsiElement.containingKtFile(): KtFile? = getStrictParentOfType()

fun PsiElement.containingKtFileImportList(): KtImportList? = containingKtFile()?.importList

fun PsiElement.addSiblingBefore(element: PsiElement): PsiElement {
  return this.parent.addBefore(element, this)
}

fun PsiElement.addSiblingAfter(element: PsiElement): PsiElement {
  return this.parent.addAfter(element, this)
}
