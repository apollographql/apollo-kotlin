package com.apollographql.apollo3.ast

/**
 * A wrapper around a schema GQLDocument that:
 * - always contain builtin types contrary to introspection that will not contain directives and SDL that will not contain
 * any builtin definitions
 * - always have a schema definition
 * - has type extensions merged
 * - has some helper functions to retrieve a type by name and/or possible types
 *
 * @param definitions a list of validated and merged definitions
 */
class Schema(
    private val definitions: List<GQLDefinition>,
) {
  val typeDefinitions: Map<String, GQLTypeDefinition> = definitions
      .filterIsInstance<GQLTypeDefinition>()
      .associateBy { it.name }

  val directiveDefinitions: Map<String, GQLDirectiveDefinition> = definitions
      .filterIsInstance<GQLDirectiveDefinition>()
      .associateBy { it.name }

  val queryTypeDefinition: GQLTypeDefinition = rootOperationTypeDefinition("query") ?: throw SchemaValidationException("No query root type found")

  val mutationTypeDefinition: GQLTypeDefinition? = rootOperationTypeDefinition("mutation")

  val subscriptionTypeDefinition: GQLTypeDefinition? = rootOperationTypeDefinition("subscription")

  fun toGQLDocument(): GQLDocument = GQLDocument(
      definitions = definitions,
      filePath = null
  ).withoutBuiltinDefinitions()

  private fun rootOperationTypeDefinition(operationType: String): GQLTypeDefinition? {
    return definitions.filterIsInstance<GQLSchemaDefinition>().single()
        .rootOperationTypeDefinitions
        .singleOrNull {
          it.operationType == operationType
        }
        ?.namedType
        ?.let { namedType ->
          definitions.filterIsInstance<GQLObjectTypeDefinition>().single { it.name == namedType }
        }
  }

  fun typeDefinition(name: String): GQLTypeDefinition {
    return typeDefinitions[name]
        ?: throw SchemaValidationException("Cannot find type `$name`")
  }

  fun possibleTypes(typeDefinition: GQLTypeDefinition): Set<String> {
    return when (typeDefinition) {
      is GQLUnionTypeDefinition -> typeDefinition.memberTypes.map { it.name }.toSet()
      is GQLInterfaceTypeDefinition -> typeDefinitions.values.filter {
        it is GQLObjectTypeDefinition && it.implementsInterfaces.contains(typeDefinition.name)
            || it is GQLInterfaceTypeDefinition && it.implementsInterfaces.contains(typeDefinition.name)
      }.flatMap {
        // Recurse until we reach the concrete types
        // This could certainly be improved
        possibleTypes(it).toList()
      }.toSet()
      is GQLObjectTypeDefinition -> setOf(typeDefinition.name)
      is GQLScalarTypeDefinition -> setOf(typeDefinition.name)
      is GQLEnumTypeDefinition -> typeDefinition.enumValues.map { it.name }.toSet()
      else -> {
        throw SchemaValidationException("Cannot determine possibleTypes of $typeDefinition.name")
      }
    }
  }

  fun possibleTypes(name: String): Set<String> {
    return possibleTypes(typeDefinition(name))
  }
}
