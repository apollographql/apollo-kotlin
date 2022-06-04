package com.apollographql.apollo3.ast.internal

import com.apollographql.apollo3.ast.*

/**
 */
internal class SchemaValidationScope(document: GQLDocument) : ValidationScope {
  override val issues = mutableListOf<Issue>()

  val documentDefinitions = document.definitions

  /**
   * The builtin definitions are required to validate directives amongst other
   * things so add them early in the validation process.
   */
  val allDefinitions = document.withBuiltinDefinitions().definitions
  override val typeDefinitions = getTypeDefinitions(allDefinitions)
  override val directiveDefinitions = getDirectives(allDefinitions)

  val schemaDefinition = getSchema(allDefinitions)

  init {
    /**
     * This is not in the specification per-se but in our use case, that will help catch some cases when users mistake
     * graphql operations for schemas
     */
    document.definitions.firstOrNull { it is GQLOperationDefinition || it is GQLFragmentDefinition }
        ?.let {
          registerIssue("Found an executable definition. Schemas should not contain operations or fragments.", it.sourceLocation)
        }
  }

  companion object {
    private fun ValidationScope.getTypeDefinitions(definitions: List<GQLDefinition>): Map<String, GQLTypeDefinition> {
      val grouped = definitions.filterIsInstance<GQLTypeDefinition>()
          .groupBy { it.name }

      grouped.values.forEach {
        val first = it.first()
        val occurences = it.map { it.sourceLocation.pretty() }.joinToString("\n")
        if (it.size > 1) {
          issues.add(
              Issue.ValidationError(
                  "type '${first.name}' is defined multiple times:\n$occurences",
                  first.sourceLocation,
                  Issue.Severity.ERROR,
                  ValidationDetails.DuplicateTypeName
              )
          )
        }
      }

      return grouped.mapValues {
        it.value.first()
      }
    }

    private fun ValidationScope.getDirectives(definitions: List<GQLDefinition>): Map<String, GQLDirectiveDefinition> {
      val grouped = definitions.filterIsInstance<GQLDirectiveDefinition>()
          .groupBy { it.name }

      grouped.values.forEach {
        val first = it.first()
        val occurences = it.map { it.sourceLocation.pretty() }.joinToString("\n")
        if (it.size > 1) {
          registerIssue(
              message = "directive '${first.name}' is defined multiple times:\n$occurences",
              sourceLocation = first.sourceLocation,
          )
        }
      }

      return grouped.mapValues {
        it.value.first()
      }
    }


    private fun ValidationScope.getSchema(definitions: List<GQLDefinition>): GQLSchemaDefinition? {
      val schemaDefinitions = definitions.filterIsInstance<GQLSchemaDefinition>()
      if (schemaDefinitions.count() > 1) {
        registerIssue(
            message = "multiple schema definitions found",
            schemaDefinitions.last().sourceLocation,
        )
      }
      return schemaDefinitions.singleOrNull()
    }
  }
}

internal fun SchemaValidationScope.validateDocumentAndMergeExtensions(): List<GQLDefinition> {
  validateNoIntrospectionNames()
  validateRootOperationTypes()

  validateInterfaces()
  validateObjects()

  val schemaDefinition = schemaDefinition ?: syntheticSchemaDefinition()

  return mergeExtensions(listOf(schemaDefinition) + allDefinitions.filter { it !is GQLSchemaDefinition })
}

internal fun SchemaValidationScope.validateRootOperationTypes() {
  schemaDefinition?.rootOperationTypeDefinitions?.forEach {
    val typeDefinition = typeDefinitions[it.namedType]
    if (typeDefinition == null) {
      registerIssue(
          "Schema defines `${it.namedType}` as root for `${it.namedType}` but `${it.namedType}` is not defined",
          sourceLocation = it.sourceLocation
      )
    }
  }
}

internal fun ValidationScope.syntheticSchemaDefinition(): GQLSchemaDefinition {
  val operationTypeDefinitions = listOf("query", "mutation", "subscription").mapNotNull {
    // 3.3.1
    // If there is no schema definition, look for an object type named after the operationType
    // i.e. Query, Mutation, ...

    // We're capitalizing manually instead of calling capitalize in case we have weird localization issues
    // For 3 string, that's ok
    val typeName = when (it) {
      "query" -> "Query"
      "mutation" -> "Mutation"
      "subscription" -> "Subscription"
      else -> error("")
    }

    val typeDefinition = typeDefinitions[typeName]
    if (typeDefinition == null) {
      if (it == "query") {
        registerIssue("No schema definition and not 'Query' type found", sourceLocation = SourceLocation.UNKNOWN)
      }
      return@mapNotNull null
    }

    GQLOperationTypeDefinition(
        operationType = it,
        namedType = typeName
    )
  }

  return GQLSchemaDefinition(
      description = null,
      directives = emptyList(),
      rootOperationTypeDefinitions = operationTypeDefinitions
  )
}

private fun ValidationScope.validateInterfaces() {
  typeDefinitions.values.filterIsInstance<GQLInterfaceTypeDefinition>().forEach {
    if (it.fields.isEmpty()) {
      registerIssue("Interfaces must specify one or more fields", it.sourceLocation)
    }
  }
}

private fun SchemaValidationScope.validateObjects() {
  typeDefinitions.values.filterIsInstance<GQLObjectTypeDefinition>().forEach { o ->
    if (o.fields.isEmpty()) {
      registerIssue("Object must specify one or more fields", o.sourceLocation)
    }

    o.implementsInterfaces.forEach { implementsInterface ->
      val iface = typeDefinitions[implementsInterface] as? GQLInterfaceTypeDefinition
      if (iface == null) {
        registerIssue("Object '${o.name}' cannot implement non-interface '$implementsInterface'", o.sourceLocation)
      }
    }

    o.directives.forEach { directive ->
      validateDirective(directive, o) {
        issues.add(it.constContextError())
      }
    }
  }
}

private fun SchemaValidationScope.validateNoIntrospectionNames() {
  // 3.3 All types and directives defined within a schema must not have a name which begins with "__"
  documentDefinitions.filterIsInstance<GQLNamed>().forEach { definition ->
    definition as GQLDefinition
    if (definition.name.startsWith("__")) {
      registerIssue("names starting with '__' are reserved for introspection", definition.sourceLocation)
    }
  }
}

private fun SchemaValidationScope.keyFields(
    typeDefinition: GQLTypeDefinition,
    allTypeDefinition: Map<String, GQLTypeDefinition>,
    keyFieldsCache: MutableMap<String, Set<String>>,
): Set<String> {
  val cached = keyFieldsCache[typeDefinition.name]
  if (cached != null) {
    return cached
  }

  val (directives, interfaces) = when (typeDefinition) {
    is GQLObjectTypeDefinition -> typeDefinition.directives to typeDefinition.implementsInterfaces
    is GQLInterfaceTypeDefinition -> typeDefinition.directives to typeDefinition.implementsInterfaces
    is GQLUnionTypeDefinition -> typeDefinition.directives to emptyList()
    else -> error("Cannot get directives for $typeDefinition")
  }

  val interfacesKeyFields = interfaces.map { keyFields(allTypeDefinition[it]!!, allTypeDefinition, keyFieldsCache) }
      .filter { it.isNotEmpty() }

  val distinct = interfacesKeyFields.distinct()
  if (distinct.size > 1) {
    val extra = interfaces.indices.map {
      "${interfaces[it]}: ${interfacesKeyFields[it]}"
    }.joinToString("\n")
    registerIssue(
        message = "Apollo: Type '${typeDefinition.name}' cannot inherit different keys from different interfaces:\n$extra",
        sourceLocation = typeDefinition.sourceLocation
    )
  }

  val keyFields = directives.toKeyFields()
  val ret = if (keyFields != null) {
    if (!distinct.isEmpty()) {
      val extra = interfaces.indices.map {
        "${interfaces[it]}: ${interfacesKeyFields[it]}"
      }.joinToString("\n")
      registerIssue(
          message = "Type '${typeDefinition.name}' cannot have key fields since it implements the following interfaces which also have key fields: $extra",
          sourceLocation = typeDefinition.sourceLocation
      )
    }
    keyFields
  } else {
    distinct.firstOrNull() ?: emptySet()
  }

  keyFieldsCache[typeDefinition.name] = ret

  return ret
}

private fun List<GQLDirective>.toKeyFields(): Set<String>? {
  val directives = filter { it.name == Schema.TYPE_POLICY }
  if (directives.isEmpty()) {
    return null
  }
  return directives.flatMap {
    (it.arguments!!.arguments.first().value as GQLStringValue).value.buffer().parseAsGQLSelections().valueAssertNoErrors().map { gqlSelection ->
      // No need to check here, this should be done during validation
      (gqlSelection as GQLField).name
    }
  }.toSet()
}

/**
 * To prevent surprising behaviour, objects that declare key fields that also implement interfaces that declare key fields are an error
 *
 * @see <a href="https://github.com/apollographql/apollo-kotlin/issues/3356#issuecomment-1134381986">Discussion</a>
 */
internal fun SchemaValidationScope.validateKeyFields(mergedDefinitions: List<GQLDefinition>): Map<String, Set<String>> {
  val keyFieldsCache = mutableMapOf<String, Set<String>>()
  val typeDefinitions = mergedDefinitions.filterIsInstance<GQLTypeDefinition>().filter { it.canHaveKeyFields() }
  typeDefinitions.forEach {
    keyFields(it, typeDefinitions.associateBy { it.name }, keyFieldsCache)
  }
  return keyFieldsCache
}