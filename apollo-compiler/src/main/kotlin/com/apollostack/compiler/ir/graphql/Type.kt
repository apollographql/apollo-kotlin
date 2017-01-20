package com.apollostack.compiler.ir.graphql

import com.apollostack.compiler.Annotations
import com.apollostack.compiler.ClassNames
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

  fun toJavaTypeName(typesPkgName: kotlin.String = "") = graphQlTypeToJavaTypeName(this, !isOptional, isOptional, typesPkgName)

  companion object {
    private val GRAPHQLTYPE_TO_JAVA_TYPE = mapOf(
        Type.String::class.java to ClassNames.STRING,
        Type.Id::class.java to ClassNames.STRING,
        Type.Int::class.java to TypeName.INT,
        Type.Float::class.java to TypeName.DOUBLE,
        Type.Boolean::class.java to TypeName.BOOLEAN)

    private fun kotlin.String.normalizeTypeName() =
        removeSuffix("!").removeSurrounding("[", "]").removeSuffix("!")

    fun resolveByName(typeName: kotlin.String, isOptional: kotlin.Boolean): Type = when {
      typeName.startsWith("String") -> Type.String(isOptional)
      typeName.startsWith("ID") -> Id(isOptional)
      typeName.startsWith("Int") -> Type.Int(isOptional)
      typeName.startsWith("Boolean") -> Type.Boolean(isOptional)
      typeName.startsWith("Float") -> Type.Float(isOptional)
      typeName.removeSuffix("!").let { it.startsWith('[') && it.endsWith(']') } -> Type.List(
          isOptional, resolveByName(typeName.normalizeTypeName(), isOptional))
      else -> Unknown(isOptional, typeName.normalizeTypeName())
    }

    fun graphQlTypeToJavaTypeName(type: Type, primitive: kotlin.Boolean, isOptional: kotlin.Boolean,
        typesPkgName: kotlin.String = ""): TypeName {
      val typeName = when (type) {
        is Type.List -> ClassNames.parameterizedListOf(graphQlTypeToJavaTypeName(type.listType, false, false, typesPkgName))
        is Unknown -> ClassName.get(if (typesPkgName.isEmpty()) "" else typesPkgName, type.typeName)
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
