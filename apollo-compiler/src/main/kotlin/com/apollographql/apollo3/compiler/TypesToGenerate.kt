package com.apollographql.apollo3.compiler

import com.apollographql.apollo3.compiler.frontend.GQLDocument
import com.apollographql.apollo3.compiler.frontend.GQLEnumTypeDefinition
import com.apollographql.apollo3.compiler.frontend.GQLInputObjectTypeDefinition
import com.apollographql.apollo3.compiler.frontend.GQLScalarTypeDefinition
import com.apollographql.apollo3.compiler.frontend.Schema
import com.apollographql.apollo3.compiler.frontend.usedTypeNames

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

  val incomingTypes = incomingMetadata?.generatedEnums?.plus(incomingMetadata.generatedInputObjects) ?: emptySet()

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

