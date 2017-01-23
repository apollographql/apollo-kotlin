package com.apollographql.android.compiler.ir.graphql

import com.apollographql.android.compiler.Annotations
import com.apollographql.android.compiler.ClassNames
import com.squareup.javapoet.ClassName
import com.squareup.javapoet.TypeName

sealed class Type(val isOptional: kotlin.Boolean) {
  class String(isOptional: kotlin.Boolean) : Type(isOptional)

  class Id(isOptional: kotlin.Boolean) : Type(isOptional)

  class Int(isOptional: kotlin.Boolean) : Type(isOptional)

  class Float(isOptional: kotlin.Boolean) : Type(isOptional)

  class Boolean(isOptional: kotlin.Boolean) : Type(isOptional)

  class List(isOptional: kotlin.Boolean, val listType: Type) : Type(isOptional)

  class Unknown(isOptional: kotlin.Boolean, val typeName: kotlin.String) : Type(isOptional)

  fun toJavaTypeName(typesPackage: kotlin.String = "") = graphQlTypeToJavaTypeName(this, !isOptional, isOptional,
      typesPackage)

  companion object {
    private val GRAPHQLTYPE_TO_JAVA_TYPE = mapOf(
        String::class.java to ClassNames.STRING,
        Id::class.java to ClassNames.STRING,
        Int::class.java to TypeName.INT,
        Float::class.java to TypeName.DOUBLE,
        Boolean::class.java to TypeName.BOOLEAN)

    private fun kotlin.String.normalizeTypeName() =
        removeSuffix("!").removeSurrounding("[", "]").removeSuffix("!")

    fun resolveByName(typeName: kotlin.String, isOptional: kotlin.Boolean): Type = when {
      typeName.startsWith("String") -> String(isOptional)
      typeName.startsWith("ID") -> Id(isOptional)
      typeName.startsWith("Int") -> Int(isOptional)
      typeName.startsWith("Boolean") -> Boolean(isOptional)
      typeName.startsWith("Float") -> Float(isOptional)
      typeName.removeSuffix("!").let { it.startsWith('[') && it.endsWith(']') } -> List(
          isOptional, resolveByName(typeName.normalizeTypeName(), isOptional))
      else -> Unknown(isOptional, typeName.normalizeTypeName())
    }

    fun graphQlTypeToJavaTypeName(type: Type, primitive: kotlin.Boolean, isOptional: kotlin.Boolean,
        typesPackage: kotlin.String = ""): TypeName {
      val typeName = when (type) {
        is List -> ClassNames.parameterizedListOf(graphQlTypeToJavaTypeName(type.listType, false, false,
            typesPackage))
        is Unknown -> ClassName.get(if (typesPackage.isEmpty()) "" else typesPackage, type.typeName)
        else ->
          GRAPHQLTYPE_TO_JAVA_TYPE[type.javaClass]!!.let {
            if (primitive) it else it.box()
          }
      }

      return if (typeName.isPrimitive) {
        typeName
      } else if (isOptional) {
        typeName.annotated(Annotations.NULLABLE)
      } else {
        typeName.annotated(Annotations.NONNULL)
      }
    }
  }
}
