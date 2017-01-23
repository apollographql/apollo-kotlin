package com.apollographql.android.compiler

import com.squareup.javapoet.ClassName
import com.squareup.javapoet.TypeName

class JavaTypeResolver(
    private val packageName: kotlin.String
) {
  fun resolve(typeName: kotlin.String, isOptional: kotlin.Boolean): TypeName {
    val normalizedTypeName = typeName.removeSuffix("!")
    val isList = normalizedTypeName.startsWith('[') && normalizedTypeName.endsWith(']')
    val javaType = when {
      isList -> ClassNames.parameterizedListOf(resolve(normalizedTypeName.removeSurrounding("[", "]"), true))
      normalizedTypeName == "String" -> ClassNames.STRING
      normalizedTypeName == "ID" -> ClassNames.STRING
      normalizedTypeName == "Int" -> if (isOptional) TypeName.INT.box() else TypeName.INT
      normalizedTypeName == "Boolean" -> if (isOptional) TypeName.BOOLEAN.box() else TypeName.BOOLEAN
      normalizedTypeName == "Float" -> if (isOptional) TypeName.DOUBLE.box() else TypeName.DOUBLE
      else -> ClassName.get(if (packageName.isEmpty()) "" else packageName, normalizedTypeName)
    }

    if (!javaType.isPrimitive) {
      return javaType.annotated(if (isOptional) Annotations.NULLABLE else Annotations.NONNULL)
    } else {
      return javaType
    }
  }
}