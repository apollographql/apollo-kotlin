package com.apollographql.ijplugin.util

import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.idea.references.KtSimpleNameReference
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtImportList
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstanceOrNull

fun PsiElement.containingKtFile(): KtFile? = getStrictParentOfType()

fun PsiElement.containingKtFileImportList(): KtImportList? = containingKtFile()?.importList

fun PsiElement.addSiblingBefore(element: PsiElement): PsiElement {
  return this.parent.addBefore(element, this)
}

fun PsiElement.addSiblingAfter(element: PsiElement): PsiElement {
  return this.parent.addAfter(element, this)
}

inline fun <reified T : PsiElement> PsiElement.findChildrenOfType(
    withSelf: Boolean = false,
    recursive: Boolean = true,
    noinline predicate: ((T) -> Boolean)? = null,
): List<T> {
  return if (recursive) {
    PsiTreeUtil.findChildrenOfAnyType(this, !withSelf, T::class.java)
  } else {
    if (withSelf && this is T) {
      listOf(this)
    } else {
      PsiTreeUtil.getChildrenOfTypeAsList(this, T::class.java)
    }
  }
      .let {
        if (predicate == null) {
          it.toList()
        } else {
          it.filter(predicate)
        }
      }
}

fun PsiElement.resolveKtName() =
    references.firstIsInstanceOrNull<KtSimpleNameReference>()?.resolve()
