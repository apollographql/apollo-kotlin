package com.apollographql.apollo3.ast

/**
 * A wrapper around a schema GQLDocument that:
 * - always contain builtin types contrary to introspection that will not contain directives and SDL that will not contain
 * any builtin definitions
 * - always has a schema definition
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

  val queryTypeDefinition: GQLTypeDefinition = rootOperationTypeDefinition("query")
      ?: throw SchemaValidationException("No query root type found")

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


  fun isTypeASubTypeOf(type: String, superType: String): Boolean {
    return implementedTypes(type).contains(superType)
  }

  fun isTypeASuperTypeOf(type: String, subType: String): Boolean {
    return implementedTypes(subType).contains(type)
  }

  /**
   * List all types (types, interfaces, unions) implemented by a given type (including itself)
   */
  fun implementedTypes(name: String): Set<String> {
    val typeDefinition = typeDefinition(name)
    return when (typeDefinition) {
      is GQLObjectTypeDefinition -> {
        val enums = typeDefinitions.values.filterIsInstance<GQLUnionTypeDefinition>().filter {
          it.memberTypes.map { it.name }.toSet().contains(typeDefinition.name)
        }.map { it.name }
        typeDefinition.implementsInterfaces.flatMap { implementedTypes(it) }.toSet() + name + enums
      }
      is GQLInterfaceTypeDefinition -> typeDefinition.implementsInterfaces.flatMap { implementedTypes(it) }.toSet() + name
      is GQLUnionTypeDefinition,
      is GQLScalarTypeDefinition,
      is GQLEnumTypeDefinition,
      -> setOf(name)
      else -> error("Cannot determine implementedTypes of $name")
    }
  }

  /**
   * Returns whether the `typePolicy` directive is present on at least one object in the schema
   */
  fun hasTypeWithTypePolicy(): Boolean {
    val directives = typeDefinitions.values.filterIsInstance<GQLObjectTypeDefinition>().flatMap { it.directives } +
        typeDefinitions.values.filterIsInstance<GQLInterfaceTypeDefinition>().flatMap { it.directives } +
        typeDefinitions.values.filterIsInstance<GQLUnionTypeDefinition>().flatMap { it.directives }
    return directives.any { it.name == TYPE_POLICY }
  }

  /**
   *  Get the key fields for an object, interface or union type.
   *  See [GQLTypeDefinition.keyFields]
   */
  fun keyFields(name: String): Set<String> {
    return typeDefinitions[name]!!.keyFields(typeDefinitions)
  }

  companion object {
    const val TYPE_POLICY = "typePolicy"
    const val FIELD_POLICY = "fieldPolicy"
    const val FIELD_POLICY_FOR_FIELD = "forField"
    const val FIELD_POLICY_KEY_ARGS = "keyArgs"
  }
}
