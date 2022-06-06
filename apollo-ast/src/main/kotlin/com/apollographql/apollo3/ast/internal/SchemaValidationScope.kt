package com.apollographql.apollo3.ast.internal

import com.apollographql.apollo3.ast.*
import com.apollographql.apollo3.ast.GQLTypeDefinition.Companion.builtInTypes
import com.apollographql.apollo3.ast.Schema.Companion.FIELD_POLICY
import com.apollographql.apollo3.ast.Schema.Companion.NONNULL
import com.apollographql.apollo3.ast.Schema.Companion.TYPE_POLICY

@Suppress("UNCHECKED_CAST")
internal fun validateSchema(definitions: List<GQLDefinition>, requiresApolloDefinitions: Boolean = false): GQLResult<Schema> {
  val issues = mutableListOf<Issue>()
  val builtinDefinitions = builtinDefinitions()
  var allDefinitions = mergeDefinitions(definitions, builtinDefinitions, ConflictResolution.TakeLeft)

  val foreignSchemas = allDefinitions.filterIsInstance<GQLSchemaExtension>()
      .getForeignSchemas(issues, builtinDefinitions)

  var foreignDefinitions = foreignSchemas.flatMap { it.definitions }

  var directivesToStrip = foreignSchemas.flatMap { it.directivesToStrip }

  if (requiresApolloDefinitions && foreignSchemas.none { it.name == "kotlin_labs" }) {
    println("""
      Apollo: using '@typePolicy', '@fieldPolicy', '@nonnull' or '@optional' will require importing them
      to avoid nameclashes in the future. To prepare for that change and remove this warning, create a 
      'extra.graphqls' file next to your schema and import the definitions:
      
      extend schema @link(url: "https://specs.apollo.dev/kotlin_labs/v0.1", import: ["@optional", "@nonnull", "@typePolicy", "@fieldPolicy"])
      
      See https://specs.apollo.dev/link/v1.0/ for more information
      """.trimIndent())

    @Suppress("DEPRECATION")
    val apolloDefinitions = apolloDefinitions()
    /**
     * Strip all directives. This will also strip schema directives like @typePolicy that should never appear in executable
     * documents
     */
    directivesToStrip = directivesToStrip + apolloDefinitions.filterIsInstance<GQLDirectiveDefinition>().map { it.name }
    foreignDefinitions = foreignDefinitions + apolloDefinitions
  }
  allDefinitions = allDefinitions + foreignDefinitions

  val directiveDefinitions = mutableMapOf<String, GQLDirectiveDefinition>()
  val typeDefinitions = mutableMapOf<String, GQLTypeDefinition>()
  val typeSystemExtensions = mutableListOf<GQLTypeSystemExtension>()
  var schemaDefinition: GQLSchemaDefinition? = null

  allDefinitions.forEach { gqlDefinition ->
    when (gqlDefinition) {
      is GQLSchemaDefinition -> {
        if (schemaDefinition != null) {
          issues.add(Issue.ValidationError("schema is already defined. First definition was ${schemaDefinition!!.sourceLocation.pretty()}", gqlDefinition.sourceLocation))
        } else {
          schemaDefinition = gqlDefinition
        }
      }
      is GQLDirectiveDefinition -> {
        val existing = typeDefinitions[gqlDefinition.name]
        if (existing != null) {
          val message = buildString {
            "Directive '${gqlDefinition.name} is defined multiple times. First definition is: ${existing.sourceLocation.pretty()}"
            if (gqlDefinition.name in setOf(TYPE_POLICY, FIELD_POLICY, "optional", NONNULL)) {
              appendLine()
              append("""
                 Use '@link' to prefix the apollo. Create a 'extra.graphqls' file next to your schema and import the definitions:
                 
                 extend schema @link(url: "https://specs.apollo.dev/kotlin_labs/v0.1", as: "apollo")
      
                See https://specs.apollo.dev/link/v1.0/ for more information
              """.trimIndent())
            }
          }
          issues.add(
              Issue.ValidationError(
                  message = message,
                  sourceLocation = gqlDefinition.sourceLocation,
                  severity = Issue.Severity.ERROR
              )
          )
        } else {
          directiveDefinitions[gqlDefinition.name] = gqlDefinition
        }
      }
      is GQLTypeDefinition -> {
        val existing = typeDefinitions[gqlDefinition.name]
        if (existing != null) {
          issues.add(Issue.ValidationError("Type '${gqlDefinition.name} is defined multiple times. First definition is: ${existing.sourceLocation.pretty()}", gqlDefinition.sourceLocation))
        } else {
          typeDefinitions[gqlDefinition.name] = gqlDefinition
        }
      }
      is GQLTypeSystemExtension -> {
        typeSystemExtensions.add(gqlDefinition)
      }
      else -> {
        /**
         * This is not in the specification per-se but in our use case, that will help catch some cases when users mistake
         * graphql operations for schemas
         */
        issues.add(Issue.ValidationError("Found an executable definition. Schemas should only contain.", gqlDefinition.sourceLocation))
      }
    }
  }

  if (schemaDefinition == null) {
    /**
     * This is not in the specification per-se but is required for `extend schema @link` usages that are not 100% spec compliant
     */
    schemaDefinition = DefaultValidationScope(typeDefinitions, directiveDefinitions, issues).syntheticSchemaDefinition()
  }

  /**
   * I'm not 100% clear on the order of validations, here I'm merging the extensions first thing
   */
  val dedupedDefinitions = listOfNotNull(schemaDefinition) + directiveDefinitions.values + typeDefinitions.values
  val dedupedScope = DefaultValidationScope(typeDefinitions, directiveDefinitions, issues)
  val mergedDefinitions = dedupedScope.mergeExtensions(dedupedDefinitions, typeSystemExtensions)

  val foreignNames = foreignSchemas.flatMap {
    it.newNames.entries
  }.associateBy(
      keySelector = { it.value },
      valueTransform = { it.key }
  )

  val mergedScope = DefaultValidationScope(
      typeDefinitions = mergedDefinitions.filterIsInstance<GQLTypeDefinition>().associateBy { it.name },
      directiveDefinitions = mergedDefinitions.filterIsInstance<GQLDirectiveDefinition>().associateBy { it.name },
      issues = issues,
      foreignNames = foreignNames,
  )
  mergedScope.validateNoIntrospectionNames()

  val mergedSchemaDefinition = mergedDefinitions.singleOrNull { it is GQLSchemaDefinition } as GQLSchemaDefinition?
  if (mergedSchemaDefinition != null) {
    mergedScope.validateRootOperationTypes(mergedSchemaDefinition)
  }

  mergedScope.validateInterfaces()
  mergedScope.validateObjects()

  val keyFields = mergedScope.validateAndComputeKeyFields()

  return if (issues.containsError()) {
    /**
     * Schema requires a valid [Query] root type which might not be always the case if there are error
     */
    GQLResult(null, issues)
  } else {
    GQLResult(
        Schema(
            mergedDefinitions,
            keyFields,
            foreignNames,
            directivesToStrip
        ),
        issues
    )
  }
}

internal fun ValidationScope.syntheticSchemaDefinition(): GQLSchemaDefinition {
  val operationTypeDefinitions = listOf("query", "mutation", "subscription").mapNotNull {
    // 3.3.1
    // If there is no schema definition, look for an object type named after the operationType
    // i.e. Query, Mutation, ...

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

/**
 * @param newNames a mapping from the foreigne name to the new name
 * @param directivesToStrip the directives to strip (using new names)
 */
private class ForeignSchema(
    val name: String,
    val definitions: List<GQLDefinition>,
    val newNames: Map<String, String>,
    val directivesToStrip: List<String>
)

/**
 * Parses the schema extensions
 *
 * Example: extend schema @link(url: "https://specs.apollo.dev/kotlin_labs/v0.1", import: ["@nonnull"])
 */
private fun List<GQLSchemaExtension>.getForeignSchemas(
    issues: MutableList<Issue>,
    builtinDefinitions: List<GQLDefinition>,
): List<ForeignSchema> {
  val schemaExtensions = this

  val foreignSchemas = mutableListOf<ForeignSchema>()

  schemaExtensions.forEach { schemaExtension ->
    schemaExtension.directives.forEach eachDirective@{ gqlDirective ->
      if (gqlDirective.name == "link") {
        val minimalSchema = builtinDefinitions + linkDefinitions()
        val scope = DefaultValidationScope(
            minimalSchema.filterIsInstance<GQLTypeDefinition>().associateBy { it.name },
            minimalSchema.filterIsInstance<GQLDirectiveDefinition>().associateBy { it.name },
        )
        scope.validateDirective(gqlDirective, schemaExtension) {
          issues.add(it.constContextError())
        }
        if (scope.issues.isNotEmpty()) {
          issues.addAll(scope.issues)
          return@eachDirective
        }

        val arguments = gqlDirective.arguments!!.arguments
        val url = (arguments.first { it.name == "url" }.value as GQLStringValue).value
        var prefix = (arguments.firstOrNull { it.name == "as" }?.value as GQLStringValue?)?.value
        val import = (arguments.firstOrNull { it.name == "import" }?.value as GQLListValue?)?.values

        var components = url.split("/")
        if (components.last().isBlank()) {
          // https://spec.example.com/a/b/c/mySchemanameidentity/v1.0
          components = components.dropLast(1)
        } else if (components.last().startsWith("?")) {
          // https://spec.example.com/a/b/c/mySchemanameidentity/v1.0/?key=val&k2=v2#frag
          components = components.dropLast(1)
        }

        if (components.size < 2) {
          issues.add(Issue.ValidationError("Invalid @link url: 'url'", gqlDirective.sourceLocation))
          return@eachDirective
        }

        val foreignName = components[components.size - 2]

        if (prefix == null) {
          prefix = foreignName
        }

        val mappings = import.orEmpty().mapNotNull {
          when (it) {
            is GQLStringValue -> it.value to it.value
            is GQLObjectValue -> {
              if (it.fields.size != 2) {
                issues.add(Issue.ValidationError("Too many fields in 'import' argument", it.sourceLocation))
                return@mapNotNull null
              }

              val name = (it.fields.firstOrNull { it.name == "name" }?.value as? GQLStringValue)?.value
              if (name == null) {
                issues.add(Issue.ValidationError("import 'name' argument is either missing or not a string", it.sourceLocation))
              }
              val as2 = (it.fields.firstOrNull { it.name == "as" }?.value as? GQLStringValue)?.value
              if (as2 == null) {
                issues.add(Issue.ValidationError("import 'as' argument is either missing or not a string", it.sourceLocation))
              }
              if (name == null || as2 == null) {
                return@mapNotNull null
              }
              name to as2
            }
            else -> {
              issues.add(Issue.ValidationError("Bad 'import' argument", it.sourceLocation))
              null
            }
          }
        }.toMap()


        if (foreignName == "kotlin_labs") {
          val (definitions, renames) = labsDefinitions().rename(mappings, prefix)
          foreignSchemas.add(
              ForeignSchema(
                  name = foreignName,
                  definitions = definitions,
                  newNames = renames,
                  directivesToStrip = definitions.filterIsInstance<GQLDirectiveDefinition>().map { it.name }
              )
          )
        }
      }
    }
  }

  return foreignSchemas
}

private fun List<GQLDefinition>.rename(mappings: Map<String, String>, prefix: String): Pair<List<GQLDefinition>, Map<String, String>> {
  val renames = mutableMapOf<String, String>()
  fun GQLNamed.newName(): String {
    return mappings.getOrDefault(name, "${prefix}__${name}").also {
      renames[name] = it
    }
  }

  val definitions = this.map { gqlDefinition ->
    gqlDefinition.transform2 { gqlNode ->
      when (gqlNode) {
        is GQLTypeDefinition -> {
          when (gqlNode) {
            is GQLScalarTypeDefinition -> gqlNode.copy(name = gqlNode.newName())
            is GQLObjectTypeDefinition -> gqlNode.copy(name = gqlNode.newName())
            is GQLEnumTypeDefinition -> gqlNode.copy(name = gqlNode.newName())
            is GQLInterfaceTypeDefinition -> gqlNode.copy(name = gqlNode.newName())
            is GQLUnionTypeDefinition -> gqlNode.copy(name = gqlNode.newName())
            is GQLInputObjectTypeDefinition -> gqlNode.copy(name = gqlNode.newName())
          }
        }
        is GQLDirectiveDefinition -> {
          // Special case, directives use a leading '@'
          val newName = mappings["@${gqlNode.name}"]?.substring(1) ?: "${prefix}__${gqlNode.name}"
          renames["@${gqlNode.name}"] = "@$newName"
          gqlNode.copy(name = newName)
        }
        is GQLNamedType -> gqlNode.copy(name = gqlNode.newName())
        else -> gqlNode
      }
    } as GQLDefinition
  }

  return definitions to renames
}

internal fun ValidationScope.validateRootOperationTypes(schemaDefinition: GQLSchemaDefinition) {
  schemaDefinition.rootOperationTypeDefinitions.forEach {
    val typeDefinition = typeDefinitions[it.namedType]
    if (typeDefinition == null) {
      registerIssue(
          "Schema defines `${it.namedType}` as root for `${it.namedType}` but `${it.namedType}` is not defined",
          sourceLocation = it.sourceLocation
      )
    }
  }
}

private fun ValidationScope.validateInterfaces() {
  typeDefinitions.values.filterIsInstance<GQLInterfaceTypeDefinition>().forEach {
    if (it.fields.isEmpty()) {
      registerIssue("Interfaces must specify one or more fields", it.sourceLocation)
    }
  }
}

private fun ValidationScope.validateObjects() {
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

    o.fields.forEach { gqlFieldDefinition ->
      gqlFieldDefinition.directives.forEach { gqlDirective ->
        validateDirective(gqlDirective, gqlFieldDefinition) {
          issues.add(it.constContextError())
        }
      }
    }
  }
}

private fun ValidationScope.validateNoIntrospectionNames() {
  // 3.3 All types and directives defined within a schema must not have a name which begins with "__"
  (typeDefinitions.values + directiveDefinitions.values).forEach { definition ->
    if (!builtInTypes.contains(definition.name) && definition.name.startsWith("__")) {
      registerIssue("names starting with '__' are reserved for introspection", definition.sourceLocation)
    }
  }
}

private fun ValidationScope.keyFields(
    typeDefinition: GQLTypeDefinition,
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

  val interfacesKeyFields = interfaces.map { keyFields(typeDefinitions[it]!!, keyFieldsCache) }
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

  val keyFields = directives.filter { originalDirectiveName(it.name) == TYPE_POLICY }.toKeyFields()
  val ret = if (keyFields != null) {
    if (distinct.isNotEmpty()) {
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
  if (isEmpty()) {
    return null
  }
  return flatMap {
    (it.arguments!!.arguments.first().value as GQLStringValue).value.buffer().parseAsGQLSelections().valueAssertNoErrors().map { gqlSelection ->
      // No need to check here, this should be done during validation
      (gqlSelection as GQLField).name
    }
  }.toSet()
}

/**
 * validate and compute the keyfield cache
 */
internal fun ValidationScope.validateAndComputeKeyFields(): Map<String, Set<String>> {
  val keyFieldsCache = mutableMapOf<String, Set<String>>()
  typeDefinitions.values.filter { it.canHaveKeyFields() }.forEach {
    keyFields(it, keyFieldsCache)
  }
  return keyFieldsCache
}