package com.apollographql.ijplugin.util

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.runReadAction
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.analyze
import org.jetbrains.kotlin.analysis.api.resolution.successfulFunctionCallOrNull
import org.jetbrains.kotlin.analysis.api.types.KaClassType
import org.jetbrains.kotlin.idea.util.application.isUnitTestMode
import org.jetbrains.kotlin.psi.KtCallElement
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtExpression
import java.util.concurrent.Callable

fun KtExpression.canBeNull(): Boolean? = runAnalyze(this) {
  expressionType?.nullability?.isNullable
}

fun KtExpression.className(): String? = runAnalyze(this) {
  expressionType.cast<KaClassType>()?.classId?.asFqNameString()
}

fun KtDeclaration.className(): String? = runAnalyze(this) {
  returnType.cast<KaClassType>()?.classId?.asFqNameString()
}

fun KtDeclaration.typeArgumentClassName(index: Int): String? = runAnalyze(this) {
  returnType.cast<KaClassType>()?.typeArguments?.getOrNull(index)?.type?.cast<KaClassType>()?.classId?.asFqNameString()
}

fun KtCallElement.getParameterNames(): List<String>? {
  return runAnalyze(this) {
    resolveToCall()?.successfulFunctionCallOrNull()?.argumentMapping?.map { it.value.name.asString() }
  }
}

/**
 * `analyze` must be called from a read action and not from the EDT.
 * In the IDE that's already the case but not in tests.
 */
private inline fun <R> runAnalyze(useSiteElement: KtElement, crossinline action: KaSession.() -> R): R {
  return if (isUnitTestMode()) {
    ApplicationManager.getApplication().executeOnPooledThread(Callable {
      runReadAction {
        analyze(useSiteElement, action)
      }
    }).get()
  } else {
    analyze(useSiteElement, action)
  }
}
