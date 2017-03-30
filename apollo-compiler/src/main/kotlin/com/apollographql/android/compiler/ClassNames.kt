package com.apollographql.android.compiler

import com.apollographql.apollo.api.Mutation
import com.apollographql.apollo.api.Operation
import com.apollographql.apollo.api.Query
import com.apollographql.apollo.api.ResponseReader
import com.apollographql.apollo.api.internal.Optional
import com.apollographql.apollo.api.internal.UnmodifiableMapBuilder
import com.apollographql.apollo.api.internal.Utils
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
  val GRAPHQL_OPERATION_VARIABLES: ClassName = ClassName.get("", "${GRAPHQL_OPERATION.simpleName()}.Variables")
  val ILLEGAL_STATE_EXCEPTION: TypeName = ClassName.get(IllegalStateException::class.java)
  val API_RESPONSE_READER: ClassName = ClassName.get(ResponseReader::class.java)
  val MAP: ClassName = ClassName.get(Map::class.java)
  val HASH_MAP: ClassName = ClassName.get(HashMap::class.java)
  val UNMODIFIABLE_MAP_BUILDER: ClassName = ClassName.get(UnmodifiableMapBuilder::class.java)
  val OPTIONAL: ClassName = ClassName.get(Optional::class.java)
  val GUAVA_OPTIONAL: ClassName = ClassName.get("com.google.common.base", "Optional")
  val API_UTILS: ClassName = ClassName.get(Utils::class.java)

  fun <K : Any> parameterizedListOf(type: Class<K>): TypeName =
      ParameterizedTypeName.get(LIST, ClassName.get(type))

  fun parameterizedListOf(typeArgument: TypeName): TypeName =
      ParameterizedTypeName.get(LIST, typeArgument.let { if (it.isPrimitive) it.box() else it.withoutAnnotations() })

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

  fun <K : Any> parameterizedOptional(type: Class<K>): TypeName =
      ParameterizedTypeName.get(OPTIONAL, ClassName.get(type))

  fun parameterizedOptional(type: TypeName): TypeName =
      ParameterizedTypeName.get(OPTIONAL, type)

  fun <K : Any> parameterizedGuavaOptional(type: Class<K>): TypeName =
      ParameterizedTypeName.get(GUAVA_OPTIONAL, ClassName.get(type))

  fun parameterizedGuavaOptional(type: TypeName): TypeName =
      ParameterizedTypeName.get(GUAVA_OPTIONAL, type)

}