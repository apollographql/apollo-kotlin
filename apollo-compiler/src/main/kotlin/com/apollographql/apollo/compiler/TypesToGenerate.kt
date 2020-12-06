package com.apollographql.apollo.compiler

import com.apollographql.apollo.compiler.frontend.gql.GQLDocument
import com.apollographql.apollo.compiler.frontend.gql.GQLEnumTypeDefinition
import com.apollographql.apollo.compiler.frontend.gql.GQLInputObjectTypeDefinition
import com.apollographql.apollo.compiler.frontend.gql.GQLScalarTypeDefinition
import com.apollographql.apollo.compiler.frontend.gql.Schema
import com.apollographql.apollo.compiler.frontend.gql.usedTypeNames

internal class TypesToGenerate(
    /**
     * The enums to generate
     */
    val generateScalarMapping: Boolean,
    /**
     * The enums to generate
     */
    val enumsToGenerate: Set<String>,
    /**
     * The enums to generate
     */
    val inputObjectsToGenerate: Set<String>,
)


internal fun computeTypesToGenerate(
    documents: List<GQLDocument>,
    schema: Schema,
    incomingMetadata: ApolloMetadata?,
    alwaysGenerateTypesMatching: Set<String>
): TypesToGenerate {
  val regexes = alwaysGenerateTypesMatching.map { Regex(it) }

  val extraTypes = schema.typeDefinitions.values.filter { typeDefinition ->
    (typeDefinition is GQLInputObjectTypeDefinition || typeDefinition is GQLEnumTypeDefinition)
        && regexes.indexOfFirst { it.matches(typeDefinition.name) } >= 0
  }.map { it.name }
      .toSet()

  val incomingTypes = incomingMetadata?.types ?: emptySet()

  val usedTypes = ((documents.flatMap { it.definitions }.usedTypeNames(schema)) + extraTypes).map {
    schema.typeDefinitions[it]!!
  }.filter {
    when (it) {
      is GQLEnumTypeDefinition,
      is GQLInputObjectTypeDefinition,
      is GQLScalarTypeDefinition -> true
      else -> false
    }
  }

  val enumsToGenerate = usedTypes.filterIsInstance<GQLEnumTypeDefinition>()
      .map { it.name }
      .filter { !incomingTypes.contains(it) }

  val inputObjectsToGenerate = usedTypes.filterIsInstance<GQLInputObjectTypeDefinition>()
      .map { it.name }
      .filter { !incomingTypes.contains(it) }


  return TypesToGenerate(
      enumsToGenerate = enumsToGenerate.toSet(),
      inputObjectsToGenerate = inputObjectsToGenerate.toSet(),
      generateScalarMapping = incomingMetadata == null,
  )
}

