package com.apollographql.ijplugin.util

import org.jetbrains.kotlin.analysis.api.analyze
import org.jetbrains.kotlin.analysis.api.types.KaClassType
import org.jetbrains.kotlin.psi.KtExpression

fun KtExpression.canBeNull(): Boolean? = analyze(this) {
  expressionType?.nullability?.isNullable
}

fun KtExpression.className(): String? = analyze(this) {
  (expressionType as? KaClassType)?.classId?.asFqNameString()
}
