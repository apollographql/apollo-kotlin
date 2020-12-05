package com.apollographql.apollo.compiler

import com.apollographql.apollo.compiler.backend.ir.BackendIrBuilder
import com.apollographql.apollo.compiler.frontend.gql.GQLDocument
import com.apollographql.apollo.compiler.frontend.gql.GQLEnumTypeDefinition
import com.apollographql.apollo.compiler.frontend.gql.GQLFragmentDefinition
import com.apollographql.apollo.compiler.frontend.gql.GQLInputObjectTypeDefinition
import com.apollographql.apollo.compiler.frontend.gql.GQLOperationDefinition
import com.apollographql.apollo.compiler.frontend.gql.GQLScalarTypeDefinition
import com.apollographql.apollo.compiler.frontend.gql.Schema
import com.apollographql.apollo.compiler.frontend.gql.usedTypeNames
import com.apollographql.apollo.compiler.frontend.gql.withTypenameWhenNeeded

internal class IRBuilder(private val schema: Schema,
                         private val schemaPackageName: String,
                         private val incomingMetadata: ApolloMetadata?,
                         private val alwaysGenerateTypesMatching: Set<String>
) {
  private fun extraTypes(): Set<String> {
    val regexes = alwaysGenerateTypesMatching.map { Regex(it) }

    return schema.typeDefinitions.values.filter { typeDefinition ->
      (typeDefinition is GQLInputObjectTypeDefinition || typeDefinition is GQLEnumTypeDefinition)
          && regexes.indexOfFirst { it.matches(typeDefinition.name) } >= 0
    }.map { it.name }
        .toSet()
  }

  internal fun build(documents: List<GQLDocument>): BackendIrBuilder.BackendIrBuilderInput {
    val documentFragmentTypeDefinitions = documents.flatMap { it.definitions.filterIsInstance<GQLFragmentDefinition>() }
    val allFragments = ((incomingMetadata?.fragments
        ?: emptyList()) + documentFragmentTypeDefinitions).map { it.withTypenameWhenNeeded(schema) }

    val fragmentsToGenerate = documentFragmentTypeDefinitions.map { it.name }

    val incomingTypes = incomingMetadata?.types ?: emptySet()
    val extraTypes = extraTypes()

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


    return BackendIrBuilder.BackendIrBuilderInput(
        operations = documents.flatMap { it.definitions.filterIsInstance<GQLOperationDefinition>() }.map {
          it.withTypenameWhenNeeded(schema)
        },
        fragments = allFragments,
        fragmentsToGenerate = fragmentsToGenerate.toSet(),
        enumsToGenerate = enumsToGenerate.toSet(),
        inputObjectsToGenerate = inputObjectsToGenerate.toSet(),
        generateScalarMapping = incomingMetadata == null,
        typesPackageName = "$schemaPackageName.type".removePrefix("."),
        fragmentsPackageName = "$schemaPackageName.fragment".removePrefix(".")
    )
  }
}
