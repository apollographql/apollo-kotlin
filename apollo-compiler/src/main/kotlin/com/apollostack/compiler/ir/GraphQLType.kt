package com.apollostack.compiler.ir

import com.apollostack.compiler.ClassNames
import com.apollostack.compiler.Annotations
import com.apollostack.compiler.normalizeTypeName
import com.squareup.javapoet.ClassName
import com.squareup.javapoet.ParameterizedTypeName
import com.squareup.javapoet.TypeName

sealed class GraphQLType(val isOptional: Boolean) {
  class GraphQLString(isOptional: Boolean) : GraphQLType(isOptional)

  class GraphQLId(isOptional: Boolean) : GraphQLType(isOptional)

  class GraphQLInt(isOptional: Boolean) : GraphQLType(isOptional)

  class GraphQLFloat(isOptional: Boolean) : GraphQLType(isOptional)

  class GraphQLBoolean(isOptional: Boolean) : GraphQLType(isOptional)

  class GraphQLList(isOptional: Boolean, val listType: GraphQLType) : GraphQLType(isOptional)

  class GraphQLUnknown(isOptional: Boolean, val typeName: String) : GraphQLType(isOptional)

  fun toJavaTypeName() = graphQlTypeToJavaTypeName(this, !isOptional, isOptional)

  companion object {
    private val GRAPHQLTYPE_TO_JAVA_TYPE = mapOf(
        GraphQLString::class.java to ClassNames.STRING,
        GraphQLId::class.java to TypeName.LONG,
        GraphQLInt::class.java to TypeName.INT,
        GraphQLFloat::class.java to TypeName.FLOAT,
        GraphQLBoolean::class.java to TypeName.BOOLEAN)

    fun resolveByName(typeName: String, isOptional: Boolean): GraphQLType = when {
      typeName.startsWith("String") -> GraphQLString(isOptional)
      typeName.startsWith("ID") -> GraphQLId(isOptional)
      typeName.startsWith("Int") -> GraphQLInt(isOptional)
      typeName.startsWith("Boolean") -> GraphQLBoolean(isOptional)
      typeName.startsWith("Float") -> GraphQLFloat(isOptional)
      typeName.removeSuffix("!").let { it.startsWith('[') && it.endsWith(']') } -> GraphQLList(
          isOptional, resolveByName(typeName.normalizeTypeName(), isOptional))
      else -> GraphQLUnknown(isOptional, typeName.normalizeTypeName())
    }

    fun graphQlTypeToJavaTypeName(
        type: GraphQLType,
        primitive: Boolean,
        isOptional: Boolean): TypeName {
      val typeName = when (type) {
        is GraphQLList -> ClassNames.parameterizedListOf(graphQlTypeToJavaTypeName(type.listType, false, false))
        is GraphQLUnknown -> ClassName.get("", type.typeName)
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
