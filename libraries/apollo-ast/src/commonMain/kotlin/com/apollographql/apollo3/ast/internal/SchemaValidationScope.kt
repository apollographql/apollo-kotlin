package com.apollographql.apollo3.ast.internal

import com.apollographql.apollo3.annotations.ApolloInternal
import com.apollographql.apollo3.ast.ConflictResolution
import com.apollographql.apollo3.ast.DirectiveRedefinition
import com.apollographql.apollo3.ast.GQLDefinition
import com.apollographql.apollo3.ast.GQLDirective
import com.apollographql.apollo3.ast.GQLDirectiveDefinition
import com.apollographql.apollo3.ast.GQLDirectiveLocation
import com.apollographql.apollo3.ast.GQLDocument
import com.apollographql.apollo3.ast.GQLEnumTypeDefinition
import com.apollographql.apollo3.ast.GQLField
import com.apollographql.apollo3.ast.GQLInputObjectTypeDefinition
import com.apollographql.apollo3.ast.GQLInterfaceTypeDefinition
import com.apollographql.apollo3.ast.GQLListValue
import com.apollographql.apollo3.ast.GQLNamed
import com.apollographql.apollo3.ast.GQLNamedType
import com.apollographql.apollo3.ast.GQLNonNullType
import com.apollographql.apollo3.ast.GQLObjectTypeDefinition
import com.apollographql.apollo3.ast.GQLObjectValue
import com.apollographql.apollo3.ast.GQLOperationTypeDefinition
import com.apollographql.apollo3.ast.GQLResult
import com.apollographql.apollo3.ast.GQLScalarTypeDefinition
import com.apollographql.apollo3.ast.GQLSchemaDefinition
import com.apollographql.apollo3.ast.GQLSchemaExtension
import com.apollographql.apollo3.ast.GQLStringValue
import com.apollographql.apollo3.ast.GQLTypeDefinition
import com.apollographql.apollo3.ast.GQLTypeDefinition.Companion.builtInTypes
import com.apollographql.apollo3.ast.GQLTypeSystemExtension
import com.apollographql.apollo3.ast.GQLUnionTypeDefinition
import com.apollographql.apollo3.ast.IncompatibleDefinition
import com.apollographql.apollo3.ast.Issue
import com.apollographql.apollo3.ast.KOTLIN_LABS_VERSION
import com.apollographql.apollo3.ast.MergeOptions
import com.apollographql.apollo3.ast.NULLABILITY_VERSION
import com.apollographql.apollo3.ast.NoQueryType
import com.apollographql.apollo3.ast.OtherValidationIssue
import com.apollographql.apollo3.ast.Schema
import com.apollographql.apollo3.ast.Schema.Companion.TYPE_POLICY
import com.apollographql.apollo3.ast.builtinDefinitions
import com.apollographql.apollo3.ast.canHaveKeyFields
import com.apollographql.apollo3.ast.combineDefinitions
import com.apollographql.apollo3.ast.findOneOf
import com.apollographql.apollo3.ast.introspection.defaultSchemaDefinition
import com.apollographql.apollo3.ast.kotlinLabsDefinitions
import com.apollographql.apollo3.ast.linkDefinitions
import com.apollographql.apollo3.ast.nullabilityDefinitions
import com.apollographql.apollo3.ast.parseAsGQLSelections
import com.apollographql.apollo3.ast.pretty
import com.apollographql.apollo3.ast.rawType
import com.apollographql.apollo3.ast.transform2

internal fun validateSchema(definitions: List<GQLDefinition>, requiresApolloDefinitions: Boolean = false): GQLResult<Schema> {
  val issues = mutableListOf<Issue>()
  val builtinDefinitions = builtinDefinitions()

  // If the builtin definitions are already in the schema, keep them
  var allDefinitions = combineDefinitions(definitions, builtinDefinitions, ConflictResolution.TakeLeft)

  val foreignSchemas = allDefinitions.filterIsInstance<GQLSchemaExtension>()
      .getForeignSchemas(issues, builtinDefinitions)

  var foreignDefinitions = foreignSchemas.flatMap { it.definitions }

  var directivesToStrip = foreignSchemas.flatMap { it.directivesToStrip }

  val kotlinLabsDefinitions = kotlinLabsDefinitions(KOTLIN_LABS_VERSION)

  if (requiresApolloDefinitions && foreignSchemas.none { it.name == "kotlin_labs" }) {
    /**
     * Strip all the apollo directives from outgoing operation documents.
     * This will also strip schema directives like @typePolicy that should never appear in executable documents
     */
    directivesToStrip = directivesToStrip + kotlinLabsDefinitions.filterIsInstance<GQLDirectiveDefinition>().map { it.name }

    /**
     * Put apolloDefinitions first so that they override the user one in the case of a conflict
     */
    foreignDefinitions = kotlinLabsDefinitions + foreignDefinitions
  }
  allDefinitions = foreignDefinitions + allDefinitions

  val directiveDefinitions = mutableMapOf<String, GQLDirectiveDefinition>()
  val typeDefinitions = mutableMapOf<String, GQLTypeDefinition>()
  val typeSystemExtensions = mutableListOf<GQLTypeSystemExtension>()
  var schemaDefinition: GQLSchemaDefinition? = null

  allDefinitions.forEach { gqlDefinition ->
    when (gqlDefinition) {
      is GQLSchemaDefinition -> {
        if (schemaDefinition != null) {
          issues.add(OtherValidationIssue("schema is already defined. First definition was ${schemaDefinition!!.sourceLocation.pretty()}", gqlDefinition.sourceLocation))
        } else {
          schemaDefinition = gqlDefinition
        }
      }

      is GQLDirectiveDefinition -> {
        val existing = directiveDefinitions[gqlDefinition.name]
        if (existing != null) {
          issues.add(
              DirectiveRedefinition(
                  name = gqlDefinition.name,
                  existing.sourceLocation,
                  sourceLocation = gqlDefinition.sourceLocation,
              )
          )
        } else {
          directiveDefinitions[gqlDefinition.name] = gqlDefinition
        }
      }

      is GQLTypeDefinition -> {
        val existing = typeDefinitions[gqlDefinition.name]
        if (existing != null) {
          issues.add(OtherValidationIssue("Type '${gqlDefinition.name}' is defined multiple times. First definition is: ${existing.sourceLocation.pretty()}", gqlDefinition.sourceLocation))
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
        issues.add(OtherValidationIssue("Found an executable definition. Schemas should only contain type system definitions.", gqlDefinition.sourceLocation))
      }
    }
  }

  nullabilityDefinitions(NULLABILITY_VERSION).forEach { definition ->
    when (definition) {
      is GQLDirectiveDefinition -> {
        val existing = directiveDefinitions[definition.name]
        if (existing != null) {
          if (!existing.semanticEquals(definition)) {
            issues.add(IncompatibleDefinition(definition.name, definition.toSemanticSdl(), existing.sourceLocation))
          }
        }
      }

      is GQLEnumTypeDefinition -> {
        val existing = typeDefinitions[definition.name]
        if (existing != null) {
          if (!existing.semanticEquals(definition)) {
            issues.add(IncompatibleDefinition(definition.name, definition.toSemanticSdl(), existing.sourceLocation))
          }
        }
      }

      else -> {}
    }
  }

  directiveDefinitions[Schema.ONE_OF]?.let {
    if (it.locations != listOf(GQLDirectiveLocation.INPUT_OBJECT) || it.arguments.isNotEmpty() || it.repeatable) {
      issues.add(IncompatibleDefinition(Schema.ONE_OF, "directive @oneOf on INPUT_OBJECT", it.sourceLocation))
    }
  }

  if (schemaDefinition == null) {
    /**
     * This is not in the specification per-se but is required for `extend schema @link` usages that are not 100% spec compliant
     */
    schemaDefinition = syntheticSchemaDefinition(typeDefinitions)
    if (schemaDefinition == null) {
      issues.add(NoQueryType("No schema definition and no query type found", null))
      return GQLResult(null, issues)
    }
  } else {
    val queryType = Schema.rootOperationTypeDefinition("query", allDefinitions)
    if (queryType == null) {
      issues.add(NoQueryType("No query type", null))
      return GQLResult(null, issues)
    }
  }

  /**
   * I'm not 100% clear on the order of validations, here I'm merging the extensions first thing
   */
  val dedupedDefinitions = listOfNotNull(schemaDefinition) + directiveDefinitions.values + typeDefinitions.values
  val mergedDefinitions = ExtensionsMerger(dedupedDefinitions + typeSystemExtensions, MergeOptions(true)).merge().getOrThrow()

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
    mergedScope.validateSchemaDefinition(mergedSchemaDefinition)
  }

  mergedScope.validateInterfaces()
  mergedScope.validateObjects()
  mergedScope.validateUnions()
  mergedScope.validateInputObjects()
  mergedScope.validateCatch(mergedSchemaDefinition)

  val keyFields = mergedScope.validateAndComputeKeyFields()
  val connectionTypes = mergedScope.computeConnectionTypes()

  return GQLResult(
      Schema(
          definitions = mergedDefinitions,
          keyFields = keyFields,
          foreignNames = foreignNames,
          directivesToStrip = directivesToStrip,
          connectionTypes = connectionTypes,
      ),
      issues
  )
}

internal fun syntheticSchemaDefinition(typeDefinitions: Map<String, GQLTypeDefinition>): GQLSchemaDefinition? {
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
        return null
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
    val directivesToStrip: List<String>,
)

/**
 * Parses the schema extensions
 *
 * Example: extend schema @link(url: "https://specs.apollo.dev/kotlin_labs/v0.3", import: ["@nonnull"])
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
        scope.validateDirectives(listOf(gqlDirective), schemaExtension) {
          issues.add(it.constContextError())
        }
        if (scope.issues.isNotEmpty()) {
          issues.addAll(scope.issues)
          return@eachDirective
        }

        val arguments = gqlDirective.arguments
        val url = (arguments.first { it.name == "url" }.value as GQLStringValue).value
        var prefix = (arguments.firstOrNull { it.name == "as" }?.value as GQLStringValue?)?.value
        val import = (arguments.firstOrNull { it.name == "import" }?.value as GQLListValue?)?.values

        var components = url.split("/")
        if (components.last().isBlank()) {
          // https://spec.example.com/a/b/c/mySchemanameidentity/v1.0/
          components = components.dropLast(1)
        } else if (components.last().startsWith("?")) {
          // https://spec.example.com/a/b/c/mySchemanameidentity/v1.0/?key=val&k2=v2#frag
          components = components.dropLast(1)
        }

        if (components.size < 2) {
          issues.add(OtherValidationIssue("Invalid @link url: 'url'", gqlDirective.sourceLocation))
          return@eachDirective
        }

        val foreignName = components[components.size - 2]
        val version = components[components.size - 1]

        if (prefix == null) {
          prefix = foreignName
        }

        val mappings = import.orEmpty().mapNotNull {
          when (it) {
            is GQLStringValue -> it.value to it.value
            is GQLObjectValue -> {
              if (it.fields.size != 2) {
                issues.add(OtherValidationIssue("Too many fields in 'import' argument", it.sourceLocation))
                return@mapNotNull null
              }

              val name = (it.fields.firstOrNull { it.name == "name" }?.value as? GQLStringValue)?.value
              if (name == null) {
                issues.add(OtherValidationIssue("import 'name' argument is either missing or not a string", it.sourceLocation))
              }
              val as2 = (it.fields.firstOrNull { it.name == "as" }?.value as? GQLStringValue)?.value
              if (as2 == null) {
                issues.add(OtherValidationIssue("import 'as' argument is either missing or not a string", it.sourceLocation))
              }
              if (name == null || as2 == null) {
                return@mapNotNull null
              }
              name to as2
            }

            else -> {
              issues.add(OtherValidationIssue("Bad 'import' argument", it.sourceLocation))
              null
            }
          }
        }.toMap()


        val foreignDefinitions = when (foreignName) {
          "kotlin_labs" -> kotlinLabsDefinitions(version)
          "nullability" -> nullabilityDefinitions(version)
          else -> null
        }

        if (foreignDefinitions != null) {
          val (definitions, renames) = foreignDefinitions.rename(mappings, prefix)
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
    return mappings.getOrDefaultMpp(name, "${prefix}__${name}").also {
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

internal fun ValidationScope.validateSchemaDefinition(schemaDefinition: GQLSchemaDefinition) {
  validateDirectives(schemaDefinition.directives.withoutLink(), schemaDefinition) {
    issues.add(it.constContextError())
  }

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

/**
 * Because @link is used to import directives, there's a chicken and egg problem and we can't validate it
 */
private fun List<GQLDirective>.withoutLink(): List<GQLDirective> {
  return this.filter {
    it.name != Schema.LINK
  }
}

private fun ValidationScope.validateInterfaces() {
  typeDefinitions.values.filterIsInstance<GQLInterfaceTypeDefinition>().forEach { i ->
    if (i.fields.isEmpty()) {
      registerIssue("Interfaces must specify one or more fields", i.sourceLocation)
    }

    i.implementsInterfaces.forEach { implementsInterface ->
      val iface = typeDefinitions[implementsInterface] as? GQLInterfaceTypeDefinition
      if (iface == null) {
        registerIssue("Interface '${i.name}' cannot implement non-interface '$implementsInterface'", i.sourceLocation)
      }
    }

    validateDirectives(i.directives, i) {
      issues.add(it.constContextError())
    }

    i.fields.forEach { gqlFieldDefinition ->
      validateDirectives(gqlFieldDefinition.directives, gqlFieldDefinition) {
        issues.add(it.constContextError())
      }
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

    validateDirectives(o.directives, o) {
      issues.add(it.constContextError())
    }

    o.fields.forEach { gqlFieldDefinition ->
      validateDirectives(gqlFieldDefinition.directives, gqlFieldDefinition) {
        issues.add(it.constContextError())
      }
    }
  }
}

private fun ValidationScope.validateUnions() {
  typeDefinitions.values.filterIsInstance<GQLUnionTypeDefinition>().forEach { u ->
    validateDirectives(u.directives, u) {
      issues.add(it.constContextError())
    }
  }
}

private fun ValidationScope.validateCatch(schemaDefinition: GQLSchemaDefinition?) {
  val hasCatchDefinition = directiveDefinitions.any {
    originalDirectiveName(it.key) == Schema.CATCH
  }

  if (!hasCatchDefinition) {
    return
  }

  if (schemaDefinition == null) {
    issues.add(OtherValidationIssue(
        message = "Schemas that include the `@catch` definition must opt-in a default CatchTo. Use `extend schema @catchByDefault(to: \$to)`",
        sourceLocation = null
    ))
    return
  }

  val catches = schemaDefinition.directives.filter {
    originalDirectiveName(it.name) == Schema.CATCH_BY_DEFAULT
  }

  if (catches.isEmpty()) {
    issues.add(OtherValidationIssue(
        message = "Schemas that include the `@catch` definition must opt-in a default CatchTo. Use `extend schema @catchByDefault(to: \$to)`",
        sourceLocation = schemaDefinition.sourceLocation
    ))
    return
  } else if (catches.size > 1) {
    issues.add(OtherValidationIssue(
        message = "There can be only one `@catch` directive on the schema definition",
        sourceLocation = schemaDefinition.sourceLocation
    ))
    return
  }
}

private fun ValidationScope.validateInputObjects() {
  typeDefinitions.values.filterIsInstance<GQLInputObjectTypeDefinition>().forEach { o ->
    if (o.inputFields.isEmpty()) {
      registerIssue("Input object must specify one or more input fields", o.sourceLocation)
    }

    validateDirectives(o.directives, o) {
      issues.add(it.constContextError())
    }

    val isOneOfInputObject = o.directives.findOneOf()
    o.inputFields.forEach { gqlInputValueDefinition ->
      if (isOneOfInputObject) {
        if (gqlInputValueDefinition.type is GQLNonNullType) {
          registerIssue("Input field '${gqlInputValueDefinition.name}' of OneOf input object '${o.name}' must be nullable", gqlInputValueDefinition.sourceLocation)
        }
        if (gqlInputValueDefinition.defaultValue != null) {
          registerIssue("Input field '${gqlInputValueDefinition.name}' of OneOf input object '${o.name}' must not have a default value", gqlInputValueDefinition.sourceLocation)
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
  val ret = if (keyFields.isNotEmpty()) {
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

private fun List<GQLDirective>.toKeyFields(): Set<String> = extractFields("keyFields")

@ApolloInternal
fun List<GQLDirective>.toEmbeddedFields(): List<String> = extractFields("embeddedFields").toList()

@ApolloInternal
fun List<GQLDirective>.toConnectionFields(): List<String> = extractFields("connectionFields").toList()

private fun List<GQLDirective>.extractFields(argumentName: String): Set<String> {
  if (isEmpty()) {
    return emptySet()
  }
  return flatMap {
    val value = it.arguments.firstOrNull {
      it.name == argumentName
    }?.value

    val selectionSet = (value as? GQLStringValue)?.value ?: return@flatMap emptyList()

    selectionSet.parseAsGQLSelections().getOrThrow().map { gqlSelection ->
      // No need to check here, this should be done during validation
      (gqlSelection as GQLField).name
    }
  }.toSet()
}

/**
 * validate and compute the keyfield cache:
 * - objects or interfaces cannot declare keyfields if they inherit and interface with keyfields
 * - objects or intefaces cannot inherit two interfaces with keyfields
 */
internal fun ValidationScope.validateAndComputeKeyFields(): Map<String, Set<String>> {
  val keyFieldsCache = mutableMapOf<String, Set<String>>()
  typeDefinitions.values.filter { it.canHaveKeyFields() }.forEach {
    keyFields(it, keyFieldsCache)
  }
  return keyFieldsCache
}

internal fun ValidationScope.computeConnectionTypes(): Set<String> {
  val connectionTypes = mutableSetOf<String>()
  for (typeDefinition in typeDefinitions.values) {
    val connectionFields = typeDefinition.directives.filter { originalDirectiveName(it.name) == TYPE_POLICY }.toConnectionFields()
    for (fieldName in connectionFields) {
      val field = typeDefinition.fields.firstOrNull { it.name == fieldName } ?: continue
      connectionTypes.add(field.type.rawType().name)
    }
  }
  return connectionTypes
}

private val GQLTypeDefinition.fields
  get() = when (this) {
    is GQLObjectTypeDefinition -> fields
    is GQLInterfaceTypeDefinition -> fields
    else -> emptyList()
  }

internal fun GQLDocument.ensureSchemaDefinition(): GQLDocument {
  if (definitions.any { it is GQLSchemaDefinition }) {
    return this
  }

  val typeDefinitions = definitions.filterIsInstance<GQLTypeDefinition>()
      .associateBy { it.name }
  return this.copy(listOf(defaultSchemaDefinition(typeDefinitions)) + definitions)
}

