package com.apollographql.ijplugin.util

import org.jetbrains.kotlin.analysis.api.analyze
import org.jetbrains.kotlin.analysis.api.types.KaClassType
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtExpression

fun KtExpression.canBeNull(): Boolean? = analyze(this) {
  expressionType?.nullability?.isNullable
}

fun KtExpression.className(): String? = analyze(this) {
  expressionType.cast<KaClassType>()?.classId?.asFqNameString()
}

fun KtDeclaration.className(): String? = analyze(this) {
  returnType.cast<KaClassType>()?.classId?.asFqNameString()
}

fun KtDeclaration.typeArgumentClassName(index: Int): String? = analyze(this) {
  returnType.cast<KaClassType>()?.typeArguments?.getOrNull(index)?.type?.cast<KaClassType>()?.classId?.asFqNameString()
}

