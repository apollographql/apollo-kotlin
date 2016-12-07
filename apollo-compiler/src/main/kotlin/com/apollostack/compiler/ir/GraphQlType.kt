package com.apollostack.compiler.ir

import com.apollostack.compiler.normalizeTypeName
import com.squareup.javapoet.AnnotationSpec
import com.squareup.javapoet.ClassName
import com.squareup.javapoet.ParameterizedTypeName
import com.squareup.javapoet.TypeName
import javax.annotation.Nullable

sealed class GraphQlType(val nullable: Boolean) {
  class GraphQlString(nullable: Boolean) : GraphQlType(nullable)

  class GraphQlId(nullable: Boolean) : GraphQlType(nullable)

  class GraphQlInt(nullable: Boolean) : GraphQlType(nullable)

  class GraphQLFloat(nullable: Boolean) : GraphQlType(nullable)

  class GraphQLBoolean(nullable: Boolean) : GraphQlType(nullable)

  class GraphQLList(nullable: Boolean, val listType: GraphQlType) : GraphQlType(nullable)

  class GraphQlUnknown(nullable: Boolean, val typeName: String) : GraphQlType(nullable)

  fun toJavaTypeName() = graphQlTypeToJavaTypeName(this, !nullable, nullable)

  companion object {
    private val NULLABLE_ANNOTATION = AnnotationSpec.builder(Nullable::class.java).build()
    private val LIST_TYPE = ClassName.get(List::class.java)
    private val GRAPHQLTYPE_TO_JAVA_TIPE = mapOf(
        GraphQlString::class.java to ClassName.get(String::class.java),
        GraphQlId::class.java to TypeName.LONG,
        GraphQlInt::class.java to TypeName.INT,
        GraphQLFloat::class.java to TypeName.FLOAT,
        GraphQLBoolean::class.java to TypeName.BOOLEAN)

    fun resolveByName(typeName: String): GraphQlType = when {
      typeName.startsWith("String") -> GraphQlString(!typeName.endsWith("!"))
      typeName.startsWith("ID") -> GraphQlId(!typeName.endsWith("!"))
      typeName.startsWith("Int") -> GraphQlInt(!typeName.endsWith("!"))
      typeName.startsWith("Boolean") -> GraphQLBoolean(!typeName.endsWith("!"))
      typeName.startsWith("Float") -> GraphQLFloat(!typeName.endsWith("!"))
      typeName.removeSuffix("!").let { it.startsWith('[') && it.endsWith(']') } -> GraphQLList(
          !typeName.endsWith("!"), resolveByName(typeName.normalizeTypeName()))
      else -> GraphQlUnknown(!typeName.endsWith("!"), typeName.normalizeTypeName())
    }

    fun graphQlTypeToJavaTypeName(
        type: GraphQlType,
        primitive: Boolean,
        nullable: Boolean): TypeName {
      val typeName = when (type) {
        is GraphQLList -> ParameterizedTypeName.get(LIST_TYPE,
            graphQlTypeToJavaTypeName(type.listType, false, false))
        is GraphQlUnknown -> ClassName.get("", type.typeName)
        else ->
          GRAPHQLTYPE_TO_JAVA_TIPE[type.javaClass]!!.let {
            if (primitive) it else it.box()
          }
      }
      return if (nullable) {
        typeName.annotated(NULLABLE_ANNOTATION)
      } else {
        typeName
      }
    }
  }
}