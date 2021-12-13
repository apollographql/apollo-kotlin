package com.apollographql.apollo3.compiler.codegen

import com.apollographql.apollo3.annotations.ApolloExperimental
import com.apollographql.apollo3.ast.GQLField
import com.apollographql.apollo3.ast.GQLInterfaceTypeDefinition
import com.apollographql.apollo3.ast.GQLObjectTypeDefinition
import com.apollographql.apollo3.ast.GQLStringValue
import com.apollographql.apollo3.ast.GQLTypeDefinition
import com.apollographql.apollo3.ast.Schema
import com.apollographql.apollo3.ast.SourceAwareException
import com.apollographql.apollo3.ast.parseAsGQLSelections
import okio.Buffer

internal fun GQLTypeDefinition.keyArgs(fieldName: String): Set<String> {
  val directives = when (this) {
    is GQLObjectTypeDefinition -> directives
    is GQLInterfaceTypeDefinition -> directives
    else -> emptyList()
  }

  return directives.filter { it.name == Schema.FIELD_POLICY }.filter {
    (it.arguments?.arguments?.single { it.name == Schema.FIELD_POLICY_FOR_FIELD }?.value as GQLStringValue).value == fieldName
  }.flatMap {
    val keyArgsValue = it.arguments?.arguments?.single { it.name == Schema.FIELD_POLICY_KEY_ARGS }?.value

    if (keyArgsValue !is GQLStringValue) {
      throw SourceAwareException("Apollo: no keyArgs found or wrong keyArgs type", it.sourceLocation)
    }

    @OptIn(ApolloExperimental::class)
    Buffer().writeUtf8(keyArgsValue.value)
        .parseAsGQLSelections()
        .value
        ?.map {
          if (it !is GQLField) {
            throw SourceAwareException("Apollo: fragments are not supported in keyArgs", it.sourceLocation)
          }
          if (it.selectionSet != null) {
            throw SourceAwareException("Apollo: composite fields are not supported in keyArgs", it.sourceLocation)
          }
          it.name
        } ?: throw SourceAwareException("Apollo: keyArgs should be a selectionSet", it.sourceLocation)
  }.toSet()
}