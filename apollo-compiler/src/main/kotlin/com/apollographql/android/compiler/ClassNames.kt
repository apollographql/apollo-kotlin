package com.apollographql.android.compiler

import com.apollographql.android.api.graphql.Mutation
import com.apollographql.android.api.graphql.Operation
import com.apollographql.android.api.graphql.Query
import com.apollographql.android.api.graphql.ResponseReader
import com.apollographql.android.api.graphql.util.UnmodifiableMapBuilder
import com.squareup.javapoet.ClassName
import com.squareup.javapoet.ParameterizedTypeName
import com.squareup.javapoet.TypeName
import java.util.*

object ClassNames {
  val STRING: ClassName = ClassName.get(String::class.java)
  val LIST: ClassName = ClassName.get(List::class.java)
  val GRAPHQL_OPERATION: ClassName = ClassName.get(Operation::class.java)
  val GRAPHQL_QUERY: ClassName = ClassName.get(Query::class.java)
  val GRAPHQL_MUTATION: ClassName = ClassName.get(Mutation::class.java)
  val GRAPHQL_OPERATION_VARIABLES: TypeName = ClassName.get("", "${GRAPHQL_OPERATION.simpleName()}.Variables")
  val ILLEGAL_STATE_EXCEPTION: TypeName = ClassName.get(IllegalStateException::class.java)
  val API_RESPONSE_READER: ClassName = ClassName.get(ResponseReader::class.java)
  val MAP: ClassName = ClassName.get(Map::class.java)
  val HASH_MAP: ClassName = ClassName.get(HashMap::class.java)
  val UNMODIFIABLE_MAP_BUILDER: ClassName = ClassName.get(UnmodifiableMapBuilder::class.java)

  fun <K : Any> parameterizedListOf(type: Class<K>): TypeName =
      ParameterizedTypeName.get(LIST, ClassName.get(type))

  fun parameterizedListOf(typeArgument: TypeName): TypeName =
      ParameterizedTypeName.get(LIST, typeArgument.withoutAnnotations())

  fun <K : Any, V : Any> parameterizedMapOf(keyTypeArgument: Class<K>, valueTypeArgument: Class<V>): TypeName =
      ParameterizedTypeName.get(MAP, ClassName.get(keyTypeArgument).withoutAnnotations(),
          ClassName.get(valueTypeArgument).withoutAnnotations())

  fun <K : Any, V : Any> parameterizedHashMapOf(keyTypeArgument: Class<K>, valueTypeArgument: Class<V>): TypeName =
      ParameterizedTypeName.get(HASH_MAP, ClassName.get(keyTypeArgument).withoutAnnotations(),
          ClassName.get(valueTypeArgument).withoutAnnotations())

  fun <K : Any, V : Any> parameterizedUnmodifiableMapBuilderOf(keyTypeArgument: Class<K>,
      valueTypeArgument: Class<V>): TypeName =
      ParameterizedTypeName.get(UNMODIFIABLE_MAP_BUILDER, ClassName.get(keyTypeArgument).withoutAnnotations(),
          ClassName.get(valueTypeArgument).withoutAnnotations())
}