package com.apollographql.ijplugin.util

import com.intellij.openapi.diagnostic.ControlFlowException
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.idea.references.KtSimpleNameReference
import org.jetbrains.kotlin.psi.KtBlockExpression
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtConstructor
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtImportList
import org.jetbrains.kotlin.psi.KtLambdaArgument
import org.jetbrains.kotlin.psi.KtNameReferenceExpression
import org.jetbrains.kotlin.psi.psiUtil.containingClass
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

fun PsiElement.resolveKtName(): PsiElement? = runCatching {
  references.firstIsInstanceOrNull<KtSimpleNameReference>()?.resolve()
}.onFailure { t ->
  // Sometimes KotlinIdeaResolutionException is thrown
  // But ControlFlowException is a normal thing to happen, so no need to log
  if (t !is ControlFlowException) logw(t, "Could not resolve $this")
}.getOrNull()

fun PsiElement.asKtClass(): KtClass? = cast<KtClass>() ?: cast<KtConstructor<*>>()?.containingClass()

fun PsiElement.originalClassName(): String? = resolveKtName()?.asKtClass()?.name

fun KtCallExpression.getMethodName(): String? = calleeExpression.cast<KtNameReferenceExpression>()?.getReferencedName()

fun KtCallExpression.lambdaBlockExpression(): KtBlockExpression? = valueArguments.firstIsInstanceOrNull<KtLambdaArgument>()?.getLambdaExpression()?.bodyExpression
