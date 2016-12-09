package com.apollostack.compiler.ir

import com.apollostack.compiler.JavaPoetUtils
import com.apollostack.compiler.normalizeTypeName
import com.squareup.javapoet.ClassName
import com.squareup.javapoet.ParameterizedTypeName
import com.squareup.javapoet.TypeName

sealed class GraphQlType(val isOptional: Boolean) {
  class GraphQlString(isOptional: Boolean) : GraphQlType(isOptional)

  class GraphQlId(isOptional: Boolean) : GraphQlType(isOptional)

  class GraphQlInt(isOptional: Boolean) : GraphQlType(isOptional)

  class GraphQLFloat(isOptional: Boolean) : GraphQlType(isOptional)

  class GraphQLBoolean(isOptional: Boolean) : GraphQlType(isOptional)

  class GraphQLList(isOptional: Boolean, val listType: GraphQlType) : GraphQlType(isOptional)

  class GraphQlUnknown(isOptional: Boolean, val typeName: String) : GraphQlType(isOptional)

  fun toJavaTypeName() = graphQlTypeToJavaTypeName(this, !isOptional, isOptional)

  companion object {
    private val LIST_TYPE = ClassName.get(List::class.java)
    private val GRAPHQLTYPE_TO_JAVA_TYPE = mapOf(
        GraphQlString::class.java to ClassName.get(String::class.java),
        GraphQlId::class.java to TypeName.LONG,
        GraphQlInt::class.java to TypeName.INT,
        GraphQLFloat::class.java to TypeName.FLOAT,
        GraphQLBoolean::class.java to TypeName.BOOLEAN)

    fun resolveByName(typeName: String, isOptional: Boolean): GraphQlType = when {
      typeName.startsWith("String") -> GraphQlString(isOptional)
      typeName.startsWith("ID") -> GraphQlId(isOptional)
      typeName.startsWith("Int") -> GraphQlInt(isOptional)
      typeName.startsWith("Boolean") -> GraphQLBoolean(isOptional)
      typeName.startsWith("Float") -> GraphQLFloat(isOptional)
      typeName.removeSuffix("!").let { it.startsWith('[') && it.endsWith(']') } -> GraphQLList(
              isOptional, resolveByName(typeName.normalizeTypeName(), isOptional))
      else -> GraphQlUnknown(isOptional, typeName.normalizeTypeName())
    }

    fun graphQlTypeToJavaTypeName(
        type: GraphQlType,
        primitive: Boolean,
        isOptional: Boolean): TypeName {
      val typeName = when (type) {
        is GraphQLList -> ParameterizedTypeName.get(LIST_TYPE,
            graphQlTypeToJavaTypeName(type.listType, false, false))
        is GraphQlUnknown -> ClassName.get("", type.typeName)
        else ->
          GRAPHQLTYPE_TO_JAVA_TYPE[type.javaClass]!!.let {
            if (primitive) it else it.box()
          }
      }
      return if (isOptional) {
        typeName.annotated(JavaPoetUtils.NULLABLE_ANNOTATION)
      } else {
        typeName
      }
    }
  }
}
