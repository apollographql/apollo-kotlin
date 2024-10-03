package com.apollographql.apollo.ast.internal

import com.apollographql.apollo.annotations.ApolloExperimental
import com.apollographql.apollo.annotations.ApolloInternal
import com.apollographql.apollo.ast.ConflictResolution
import com.apollographql.apollo.ast.DirectiveRedefinition
import com.apollographql.apollo.ast.ForeignSchema
import com.apollographql.apollo.ast.GQLDefinition
import com.apollographql.apollo.ast.GQLDirective
import com.apollographql.apollo.ast.GQLDirectiveDefinition
import com.apollographql.apollo.ast.GQLDirectiveLocation
import com.apollographql.apollo.ast.GQLDocument
import com.apollographql.apollo.ast.GQLEnumTypeDefinition
import com.apollographql.apollo.ast.GQLField
import com.apollographql.apollo.ast.GQLInputObjectTypeDefinition
import com.apollographql.apollo.ast.GQLInterfaceTypeDefinition
import com.apollographql.apollo.ast.GQLListValue
import com.apollographql.apollo.ast.GQLNamed
import com.apollographql.apollo.ast.GQLNamedType
import com.apollographql.apollo.ast.GQLNonNullType
import com.apollographql.apollo.ast.GQLObjectTypeDefinition
import com.apollographql.apollo.ast.GQLObjectValue
import com.apollographql.apollo.ast.GQLOperationTypeDefinition
import com.apollographql.apollo.ast.GQLResult
import com.apollographql.apollo.ast.GQLScalarTypeDefinition
import com.apollographql.apollo.ast.GQLSchemaDefinition
import com.apollographql.apollo.ast.GQLSchemaExtension
import com.apollographql.apollo.ast.GQLStringValue
import com.apollographql.apollo.ast.GQLTypeDefinition
import com.apollographql.apollo.ast.GQLTypeDefinition.Companion.builtInTypes
import com.apollographql.apollo.ast.GQLTypeSystemExtension
import com.apollographql.apollo.ast.GQLUnionTypeDefinition
import com.apollographql.apollo.ast.GQLValue
import com.apollographql.apollo.ast.IncompatibleDefinition
import com.apollographql.apollo.ast.Issue
import com.apollographql.apollo.ast.KOTLIN_LABS_VERSION
import com.apollographql.apollo.ast.MergeOptions
import com.apollographql.apollo.ast.NULLABILITY_VERSION
import com.apollographql.apollo.ast.NoQueryType
import com.apollographql.apollo.ast.OtherValidationIssue
import com.apollographql.apollo.ast.Schema
import com.apollographql.apollo.ast.Schema.Companion.TYPE_POLICY
import com.apollographql.apollo.ast.builtinDefinitions
import com.apollographql.apollo.ast.canHaveKeyFields
import com.apollographql.apollo.ast.combineDefinitions
import com.apollographql.apollo.ast.findOneOf
import com.apollographql.apollo.ast.introspection.defaultSchemaDefinition
import com.apollographql.apollo.ast.kotlinLabsDefinitions
import com.apollographql.apollo.ast.linkDefinitions
import com.apollographql.apollo.ast.nullabilityDefinitions
import com.apollographql.apollo.ast.parseAsGQLSelections
import com.apollographql.apollo.ast.pretty
import com.apollographql.apollo.ast.rawType
import com.apollographql.apollo.ast.transform2

/**
 * @param addKotlinLabsDefinitions automatically import all the kotlin_labs definitions, even if no `@link` is present
 * @param foreignSchemas a list of known [ForeignSchema] that may or may not be imported depending on the `@link` directives
 */
@ApolloExperimental
class SchemaValidationOptions(
    val addKotlinLabsDefinitions: Boolean,
    val foreignSchemas: List<ForeignSchema>,
)

internal fun validateSchema(definitions: List<GQLDefinition>, options: SchemaValidationOptions): GQLResult<Schema> {
  val issues = mutableListOf<Issue>()
  val builtinDefinitions = builtinDefinitions()

  // If the builtin definitions are already in the schema, keep them
  var allDefinitions = combineDefinitions(definitions, builtinDefinitions, ConflictResolution.TakeLeft)

  val imports = allDefinitions.filterIsInstance<GQLSchemaExtension>()
      .getImports(issues, builtinDefinitions, options.foreignSchemas)

  var foreignDefinitions = imports.flatMap { it.renamedDefinitions }

  var directivesToStrip = imports.flatMap { it.foreignSchema.directivesToStrip }

  val kotlinLabsDefinitions = kotlinLabsDefinitions(KOTLIN_LABS_VERSION)

  if (options.addKotlinLabsDefinitions && imports.none { it.foreignSchema.name == "kotlin_labs" }) {
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

  val foreignNames = imports.flatMap {
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
 * @param foreignSchema the [ForeignSchema] being imported.
 * @param renamedDefinitions [ForeignSchema.definitions] renamed according to the import.
 * By default, the new names are `${foreignSchemaName}__${definitionName}`
 * @param newNames a mapping from the initial foreign name to the new name.
 */
private class Import(
    val foreignSchema: ForeignSchema,
    val renamedDefinitions: List<GQLDefinition>,
    val newNames: Map<String, String>,
)

private class UrlParseResult(val name: String, val version: String)

private fun String.parseLink(): UrlParseResult? {
  var components = split("/")
  if (components.last().isBlank()) {
    // https://spec.example.com/a/b/c/mySchemanameidentity/v1.0/
    components = components.dropLast(1)
  } else if (components.last().startsWith("?")) {
    // https://spec.example.com/a/b/c/mySchemanameidentity/v1.0/?key=val&k2=v2#frag
    components = components.dropLast(1)
  }

  if (components.size < 2) {
    return null
  }

  return UrlParseResult(components[components.size - 2], components[components.size - 1])
}

/**
 * Parses the `@link` schema extensions.
 *
 * Example: extend schema @link(url: "https://specs.apollo.dev/nullability/v0.4/", import: ["@catchByDefault", "CatchTo"])
 */
private fun List<GQLSchemaExtension>.getImports(
    issues: MutableList<Issue>,
    builtinDefinitions: List<GQLDefinition>,
    foreignSchemas: List<ForeignSchema>,
): List<Import> {
  val schemaExtensions = this

  val imports = mutableListOf<Import>()

  schemaExtensions.forEach { schemaExtension ->
    schemaExtension.directives.forEach eachDirective@{ gqlDirective ->
      if (gqlDirective.name == "link") {
        /**
         * Validate `@link` using a very minimal schema.
         * This ensure we can safely cast the arguments below
         */
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

        val urlParseResult = url.parseLink()
        if (urlParseResult == null) {
          issues.add(OtherValidationIssue("Invalid @link url: 'url'", gqlDirective.sourceLocation))
          return@eachDirective
        }
        val foreignName = urlParseResult.name
        val foreignVersion = urlParseResult.version

        var prefix = (arguments.firstOrNull { it.name == "as" }?.value as GQLStringValue?)?.value
        if (prefix == null) {
          prefix = urlParseResult.name
        }

        val import = (arguments.firstOrNull { it.name == "import" }?.value as GQLListValue?)?.values
        val mappings = import.orEmpty().parseImport(issues)

        val foreignSchema = foreignSchemas.firstOrNull { it.name == foreignName && it.version == foreignVersion }

        if (foreignSchema != null) {
          mappings.keys.forEach { key ->
            if (foreignSchema.definitions.none { it.displayName() == key }) {
              issues.add(
                  OtherValidationIssue("Apollo: unknown definition '$key'", gqlDirective.sourceLocation)
              )
            }
          }

          val (definitions, renames) = foreignSchema.definitions.rename(mappings, prefix)
          imports.add(
              Import(
                  foreignSchema = foreignSchema,
                  renamedDefinitions = definitions,
                  newNames = renames,
              )
          )
        } else {
          issues.add(OtherValidationIssue("Apollo: unknown foreign schema '$foreignName/$foreignVersion'", gqlDirective.sourceLocation))
        }
      }
    }
  }

  return imports
}

/**
 * Parses the import argument of `@link`:
 *
 * ```
 * extend schema
 *   @link(url: "https://example.com/otherSchema",
 *     # @link infers a name from the URL
 *     #   (use as: to set it explicitly)
 *     import: ["SomeType", "@someDirective", {
 *       name: "@someRenamedDirective",
 *       as: "@renamed"
 *     }])
 * ```
 *
 * @return a mapping from the original name in the definitions to the new name. For the example above:
 * ```kotlin
 * mapOf(
 *    "SomeType" to "SomeType",
 *    "@someDirective" to "@someDirective",
 *    "@someRenamedDirective" to "@renamed",
 * )
 * ```
 */
private fun List<GQLValue>.parseImport(issues: MutableList<Issue>): Map<String, String> {
  return mapNotNull {
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
          val newName = mappings[gqlNode.displayName()]?.substring(1) ?: "${prefix}__${gqlNode.name}"
          renames[gqlNode.displayName()] = "@$newName"
          gqlNode.copy(name = newName)
        }

        is GQLNamedType -> gqlNode.copy(name = gqlNode.newName())
        else -> gqlNode
      }
    } as GQLDefinition
  }

  return definitions to renames
}

/**
 * Because types and directive may share the same name, they are disambiguated
 */
private fun GQLDefinition.displayName(): String {
  return when (this) {
    is GQLDirectiveDefinition -> {
      "@${name}"
    }

    is GQLNamed -> {
      name
    }

    else -> {
      /**
       * This case happens if a foreign schema adds a [GQLSchemaDefinition].
       * This is very unlikely to happen but if it does, do not crash.
       */
      ""
    }
  }
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
        message = "Schemas that include nullability directives must opt-in a default CatchTo. Use `extend schema @catchByDefault(to: \$to)`",
        sourceLocation = null
    )
    )
    return
  }

  val catches = schemaDefinition.directives.filter {
    originalDirectiveName(it.name) == Schema.CATCH_BY_DEFAULT
  }

  if (catches.isEmpty()) {
    issues.add(OtherValidationIssue(
        message = "Schemas that include nullability directives must opt-in a default CatchTo. Use `extend schema @catchByDefault(to: \$to)`",
        sourceLocation = schemaDefinition.sourceLocation
    )
    )
    return
  } else if (catches.size > 1) {
    issues.add(OtherValidationIssue(
        message = "There can be only one `@catch` directive on the schema definition",
        sourceLocation = schemaDefinition.sourceLocation
    )
    )
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

