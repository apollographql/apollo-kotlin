package com.apollographql.apollo3.ast

import com.apollographql.apollo3.annotations.ApolloExperimental
import com.apollographql.apollo3.ast.internal.buffer

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


  /**
   * List all types (types, interfaces, unions) implemented by a given type
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
    return typeDefinitions.values.filterIsInstance<GQLObjectTypeDefinition>().any { objectType ->
      objectType.directives.any { it.name == TYPE_POLICY }
    }
  }

  /**
   * Returns the key fields for the given type
   *
   * If this type has one or multiple @[TYPE_POLICY] annotation(s), they are used, else it recurses in implemented interfaces until it
   * finds some.
   *
   * Returns the emptySet if this type has no key fields.
   */
  fun keyFields(name: String): Set<String> {
    val typeDefinition = typeDefinition(name)
    return when (typeDefinition) {
      is GQLObjectTypeDefinition -> {
        val kf = typeDefinition.directives.toKeyFields()
        if (kf != null) {
          kf
        } else {
          val kfs = typeDefinition.implementsInterfaces.map { it to keyFields(it) }.filter { it.second.isNotEmpty() }
          if (kfs.isNotEmpty()) {
            check(kfs.size == 1) {
              val candidates = kfs.map { "${it.first}: ${it.second}" }.joinToString("\n")
              "Object '$name' inherits different keys from different interfaces:\n$candidates\nSpecify @$TYPE_POLICY explicitely"
            }
          }
          kfs.singleOrNull()?.second ?: emptySet()
        }
      }
      is GQLInterfaceTypeDefinition -> {
        val kf = typeDefinition.directives.toKeyFields()
        if (kf != null) {
          kf
        } else {
          val kfs = typeDefinition.implementsInterfaces.map { it to keyFields(it) }.filter { it.second.isNotEmpty() }
          if (kfs.isNotEmpty()) {
            check(kfs.size == 1) {
              val candidates = kfs.map { "${it.first}: ${it.second}" }.joinToString("\n")
              "Interface '$name' inherits different keys from different interfaces:\n$candidates\nSpecify @$TYPE_POLICY explicitely"
            }
          }
          kfs.singleOrNull()?.second ?: emptySet()
        }
      }
      is GQLUnionTypeDefinition -> typeDefinition.directives.toKeyFields() ?: emptySet()
      else -> error("Type '$name' cannot have key fields")
    }
  }

  /**
   * Returns the key Fields or null if there's no directive
   */
  private fun List<GQLDirective>.toKeyFields(): Set<String>? {
    val directives = filter { it.name == TYPE_POLICY }
    if (directives.isEmpty()) {
      return null
    }
    @OptIn(ApolloExperimental::class)
    return directives.flatMap {
      (it.arguments!!.arguments.first().value as GQLStringValue).value.buffer().parseAsGQLSelections().valueAssertNoErrors().map { gqlSelection ->
        // No need to check here, this should be done during validation
        (gqlSelection as GQLField).name
      }
    }.toSet()
  }

  companion object {
    const val TYPE_POLICY = "typePolicy"
    const val FIELD_POLICY = "fieldPolicy"
    const val FIELD_POLICY_FOR_FIELD = "forField"
    const val FIELD_POLICY_KEY_ARGS = "keyArgs"
  }
}
