package com.apollostack.compiler

import com.apollostack.api.Query
import com.squareup.javapoet.ClassName
import com.squareup.javapoet.ParameterizedTypeName
import com.squareup.javapoet.TypeName
import java.util.*

object ClassNames {
  val STRING: ClassName = ClassName.get(String::class.java)
  val LIST: ClassName = ClassName.get(List::class.java)
  val COLLECTIONS: ClassName = ClassName.get(Collections::class.java)
  val ARRAYS: ClassName = ClassName.get(Arrays::class.java)
  val OBJECT: ClassName = ClassName.get(Object::class.java)
  val MAP: ClassName = ClassName.get(Map::class.java)
  val HASH_MAP: ClassName = ClassName.get(HashMap::class.java)
  val STRING_BUILDER: ClassName = ClassName.get(StringBuilder::class.java)
  val API_QUERY: ClassName = ClassName.get(Query::class.java)
  val API_QUERY_VARIABLES: TypeName = ClassName.get("", "${API_QUERY.simpleName()}.Variables")

  fun parameterizedMapOf(firstTypeArgument: TypeName, secondTypeArgument: TypeName): TypeName =
      ParameterizedTypeName.get(MAP, firstTypeArgument, secondTypeArgument)

  fun parameterizedHashMapOf(firstTypeArgument: TypeName, secondTypeArgument: TypeName): TypeName =
      ParameterizedTypeName.get(HASH_MAP, firstTypeArgument, secondTypeArgument)

  fun parameterizedListOf(typeArgument: TypeName): TypeName =
      ParameterizedTypeName.get(LIST, typeArgument)
}