package com.apollostack.compiler

import com.apollostack.api.GraphQLMutation
import com.apollostack.api.GraphQLOperation
import com.apollostack.api.GraphQLQuery
import com.squareup.javapoet.ClassName
import com.squareup.javapoet.ParameterizedTypeName
import com.squareup.javapoet.TypeName
import com.squareup.javapoet.WildcardTypeName

object ClassNames {
  val STRING: ClassName = ClassName.get(String::class.java)
  val LIST: ClassName = ClassName.get(List::class.java)
  @Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")
  val GRAPHQL_OPERATION: ClassName = ClassName.get(GraphQLOperation::class.java)
  val GRAPHQL_QUERY: ClassName = ClassName.get(GraphQLQuery::class.java)
  val GRAPHQL_MUTATION: ClassName = ClassName.get(GraphQLMutation::class.java)
  val GRAPHQL_OPERATION_VARIABLES: TypeName = ClassName.get("", "${GRAPHQL_OPERATION.simpleName()}.Variables")
  val ILLEGAL_STATE_EXCEPTION: TypeName = ClassName.get(IllegalStateException::class.java)

  fun parameterizedListOf(typeArgument: TypeName): TypeName =
      ParameterizedTypeName.get(LIST, WildcardTypeName.subtypeOf(typeArgument.withoutAnnotations()))
}