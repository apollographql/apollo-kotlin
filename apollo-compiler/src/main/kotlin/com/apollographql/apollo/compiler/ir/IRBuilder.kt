package com.apollographql.apollo.compiler.ir

import com.apollographql.apollo.compiler.ApolloMetadata
import com.apollographql.apollo.compiler.PackageNameProvider
import com.apollographql.apollo.compiler.parser.gql.GQLDocument
import com.apollographql.apollo.compiler.parser.gql.GQLEnumTypeDefinition
import com.apollographql.apollo.compiler.parser.gql.GQLFragmentDefinition
import com.apollographql.apollo.compiler.parser.gql.GQLInputObjectTypeDefinition
import com.apollographql.apollo.compiler.parser.gql.GQLOperationDefinition
import com.apollographql.apollo.compiler.parser.gql.GQLScalarTypeDefinition
import com.apollographql.apollo.compiler.parser.gql.Schema
import com.apollographql.apollo.compiler.parser.gql.isBuiltIn
import com.apollographql.apollo.compiler.parser.gql.toIR
import com.apollographql.apollo.compiler.parser.gql.usedTypeNames
import com.apollographql.apollo.compiler.parser.gql.withTypenameWhenNeeded

class IRBuilder(private val schema: Schema,
                private val schemaPackageName: String,
                private val incomingMetadata: ApolloMetadata?,
                private val alwaysGenerateTypesMatching: Set<String>,
                val packageNameProvider: PackageNameProvider
) {
  private fun extraTypes(): Set<String> {
    val regexes = alwaysGenerateTypesMatching.map { Regex(it) }

    return schema.typeDefinitions.values.filter { typeDefinition ->
      (typeDefinition is GQLInputObjectTypeDefinition || typeDefinition is GQLEnumTypeDefinition)
          && regexes.indexOfFirst { it.matches(typeDefinition.name) } >= 0
    }.map { it.name }
        .toSet()
  }

  fun build(documents: List<GQLDocument>): CodeGenerationIR {
    val documentFragmentTypeDefinitions = documents.flatMap { it.definitions.filterIsInstance<GQLFragmentDefinition>() }
    val allFragments = ((incomingMetadata?.fragments ?: emptyList()) + documentFragmentTypeDefinitions).map { it.withTypenameWhenNeeded(schema) }

    val fragmentsToGenerate = documentFragmentTypeDefinitions.map { it.name }

    val incomingTypes = incomingMetadata?.types ?: emptySet()
    val extraTypes = extraTypes()

    val typeDeclarations = ((documents.flatMap { it.definitions }.usedTypeNames(schema)) + extraTypes).map {
      schema.typeDefinitions[it]!!
    }.filter {
      when (it) {
        is GQLEnumTypeDefinition,
        is GQLInputObjectTypeDefinition -> true
        else -> false
      }
    }

    val enumsToGenerate = typeDeclarations.filterIsInstance<GQLEnumTypeDefinition>()
        .map { it.name }
        .filter { !incomingTypes.contains(it) }

    val inputObjectsToGenerate = typeDeclarations.filterIsInstance<GQLInputObjectTypeDefinition>()
        .map { it.name }
        .filter { !incomingTypes.contains(it) }

    val scalarsToGenerate = when {
      // multi-module -> the scalar types will be already generated
      incomingMetadata != null -> emptyList()
      // non multi-module or root -> generate all scalar types
      else -> schema.typeDefinitions
          .values
          .filterIsInstance<GQLScalarTypeDefinition>()
          .filter{ !it.isBuiltIn() }
          .map { it.name } + "ID" // not sure why we need to add ID there
    }

    return CodeGenerationIR(
        operations = documents.flatMap { it.definitions.filterIsInstance<GQLOperationDefinition>() }.map {
          it.withTypenameWhenNeeded(schema).toIR(schema, allFragments.associateBy { it.name }, packageNameProvider)
        },
        fragments = allFragments.map { it.toIR(schema, allFragments.associateBy { it.name }) },
        typeDeclarations = typeDeclarations.map { it.toIR(schema) },
        fragmentsToGenerate = fragmentsToGenerate.toSet(),
        enumsToGenerate = enumsToGenerate.toSet(),
        inputObjectsToGenerate = inputObjectsToGenerate.toSet(),
        scalarsToGenerate = scalarsToGenerate.toSet(),
        typesPackageName = "$schemaPackageName.type".removePrefix("."),
        fragmentsPackageName = "$schemaPackageName.fragment".removePrefix(".")
    )
  }
}