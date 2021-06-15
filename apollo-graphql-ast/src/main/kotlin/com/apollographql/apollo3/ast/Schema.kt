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


  fun implementedTypes(name: String): Set<String> {
    val typeDefinition = typeDefinition(name)
    return when (typeDefinition) {
      is GQLObjectTypeDefinition -> typeDefinition.implementsInterfaces.flatMap { implementedTypes(it) }.toSet() + name
      is GQLInterfaceTypeDefinition -> typeDefinition.implementsInterfaces.flatMap { implementedTypes(it) }.toSet() + name
      is GQLUnionTypeDefinition,
      is GQLScalarTypeDefinition,
      is GQLEnumTypeDefinition,
      -> setOf(name)
      else -> error("Cannot determine implementedTypes of $name")
    }
  }

  fun keyFields(name: String): Set<String> {
    val keyFieldsNoFallback = keyFieldsNoFallback(name)
    if (keyFieldsNoFallback != null) {
      return keyFieldsNoFallback
    }

    val schemaDefinition =  definitions.filterIsInstance<GQLSchemaDefinition>().single()
    val schemaKeyFields = schemaDefinition.directives.toKeyFields("defaultKeyFields")
    if (schemaKeyFields == null) {
      return emptySet()
    }

    val typeDefinition = typeDefinition(name)
    val fields = when(typeDefinition) {
      is GQLObjectTypeDefinition -> typeDefinition.fields
      is GQLInterfaceTypeDefinition -> typeDefinition.fields
      else -> return emptySet()
    }

    return fields.mapNotNull {
      if (schemaKeyFields.contains(it.name)) {
        val fieldTypeDefinition = typeDefinition(it.type.leafType().name)
        check(fieldTypeDefinition is GQLScalarTypeDefinition || fieldTypeDefinition is GQLEnumTypeDefinition) {
          "Compound keyFields are not supported for field '${it.name}' of type '${it.type.leafType().name}'"
        }
        it.name
      } else {
        null
      }
    }.toSet()
  }

  private fun keyFieldsNoFallback(name: String): Set<String>? {
    val typeDefinition = typeDefinition(name)
    return when (typeDefinition) {
      is GQLObjectTypeDefinition -> {
        val kf = typeDefinition.directives.toKeyFields()
        if (kf != null) {
          kf
        } else {
          val kfs = typeDefinition.implementsInterfaces.map { it to keyFieldsNoFallback(it) }.filter { it.second != null }
          if (kfs.isNotEmpty()) {
            check(kfs.size == 1) {
              val candidates = kfs.map { "${it.first}: ${it.second}" }.joinToString("\n")
              "Object '$name' inherits different keys from different interfaces:\n$candidates\nSpecify @key explicitely"
            }
          }
          kfs.singleOrNull()?.second
        }
      }
      is GQLInterfaceTypeDefinition -> {
        val kf = typeDefinition.directives.toKeyFields()
        if (kf != null) {
          kf
        } else {
          val kfs = typeDefinition.implementsInterfaces.map { it to keyFieldsNoFallback(it) }.filter { it.second != null }
          if (kfs.isNotEmpty()) {
            check(kfs.size == 1) {
              val candidates = kfs.map { "${it.first}: ${it.second}" }.joinToString("\n")
              "Interface '$name' inherits different keys from different interfaces:\n$candidates\nSpecify @key explicitely"
            }
          }
          kfs.singleOrNull()?.second
        }
      }
      is GQLUnionTypeDefinition -> typeDefinition.directives.toKeyFields()
      else -> error("Type '$name' cannot have key fields")
    }
  }

  private fun List<GQLDirective>.toKeyFields(directiveName: String = "key"): Set<String>? {
    val directives = filter { it.name == directiveName }
    if (directives.isEmpty()) {
      return null
    }
    return directives.flatMap {
      (it.arguments!!.arguments.first().value as GQLStringValue).value.parseAsSelections().getOrThrow().map {
        (it as GQLField).name
      }
    }.toSet()
  }
}
