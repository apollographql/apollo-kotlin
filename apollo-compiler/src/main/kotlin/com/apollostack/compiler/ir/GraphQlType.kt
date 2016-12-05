package com.apollostack.compiler.ir

import com.squareup.javapoet.ClassName
import com.squareup.javapoet.ParameterizedTypeName
import com.squareup.javapoet.TypeName

sealed class GraphQlType(val nullable: Boolean) {

  class GraphQlString(nullable: Boolean) : GraphQlType(nullable)

  class GraphQlId(nullable: Boolean) : GraphQlType(nullable)

  class GraphQlInt(nullable: Boolean) : GraphQlType(nullable)

  class GraphQLFloat(nullable: Boolean) : GraphQlType(nullable)

  class GraphQLBoolean(nullable: Boolean) : GraphQlType(nullable)

  class GraphQLList(nullable: Boolean, val listType: GraphQlType) : GraphQlType(nullable)

  class GraphQlUnknown(nullable: Boolean, val typeName: String) : GraphQlType(nullable)

  fun toJavaTypeName(): TypeName {
    return when (this) {
      is GraphQlString -> ClassName.get(String::class.java)
      is GraphQlId -> if (nullable) ClassName.get(java.lang.Long::class.java) else ClassName.LONG
      is GraphQlInt -> if (nullable) ClassName.get(java.lang.Integer::class.java) else TypeName.INT
      is GraphQLFloat -> if (nullable) ClassName.get(java.lang.Float::class.java) else TypeName.FLOAT
      is GraphQLBoolean -> if (nullable) ClassName.get(java.lang.Boolean::class.java) else TypeName.BOOLEAN
      is GraphQLList -> ParameterizedTypeName.get(ClassName.get(List::class.java), listType.toJavaTypeName())
      is GraphQlUnknown -> ClassName.get("", typeName)
    }
  }

  companion object {

    private fun String.normalizeTypeName() = removeSuffix("!").removeSurrounding(prefix = "[", suffix = "]").removeSuffix("!")

    fun resolveByName(typeName: String): GraphQlType = when {
      typeName.startsWith("String") -> GraphQlString(!typeName.endsWith("!"))
      typeName.startsWith("ID") -> GraphQlId(!typeName.endsWith("!"))
      typeName.startsWith("Int") -> GraphQlInt(!typeName.endsWith("!"))
      typeName.startsWith("Boolean") -> GraphQLBoolean(!typeName.endsWith("!"))
      typeName.startsWith("Float") -> GraphQLFloat(!typeName.endsWith("!"))
      typeName.removeSuffix("!").let { it.startsWith('[') && it.endsWith(']') } -> GraphQLList(!typeName.endsWith("!"), resolveByName(typeName.normalizeTypeName()))
      else -> GraphQlUnknown(!typeName.endsWith("!"), typeName.normalizeTypeName())
    }
  }
}