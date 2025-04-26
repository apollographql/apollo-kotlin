package com.apollographql.apollo.ast.internal

import com.apollographql.apollo.annotations.ApolloExperimental
import com.apollographql.apollo.annotations.ApolloInternal
import com.apollographql.apollo.ast.DirectiveRedefinition
import com.apollographql.apollo.ast.ForeignSchema
import com.apollographql.apollo.ast.GQLDefinition
import com.apollographql.apollo.ast.GQLDirective
import com.apollographql.apollo.ast.GQLDirectiveDefinition
import com.apollographql.apollo.ast.GQLDocument
import com.apollographql.apollo.ast.GQLEnumTypeDefinition
import com.apollographql.apollo.ast.GQLField
import com.apollographql.apollo.ast.GQLFieldDefinition
import com.apollographql.apollo.ast.GQLInputObjectTypeDefinition
import com.apollographql.apollo.ast.GQLInputValueDefinition
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
import com.apollographql.apollo.ast.MergeOptions
import com.apollographql.apollo.ast.NoQueryType
import com.apollographql.apollo.ast.OtherValidationIssue
import com.apollographql.apollo.ast.Schema
import com.apollographql.apollo.ast.Schema.Companion.TYPE_POLICY
import com.apollographql.apollo.ast.SourceLocation
import com.apollographql.apollo.ast.autoLinkedKotlinLabsForeignSchema
import com.apollographql.apollo.ast.builtinDefinitions
import com.apollographql.apollo.ast.canHaveKeyFields
import com.apollographql.apollo.ast.findOneOf
import com.apollographql.apollo.ast.introspection.defaultSchemaDefinition
import com.apollographql.apollo.ast.linkDefinitions
import com.apollographql.apollo.ast.parseAsGQLDocument
import com.apollographql.apollo.ast.parseAsGQLSelections
import com.apollographql.apollo.ast.rawType
import com.apollographql.apollo.ast.transform2
import com.apollographql.apollo.ast.withBuiltinDefinitions

/**
 * @param addKotlinLabsDefinitions automatically import all the kotlin_labs definitions, even if no `@link` is present
 * @param foreignSchemas a list of known [ForeignSchema] that may or may not be imported depending on the `@link` directives
 */
@ApolloExperimental
class SchemaValidationOptions(
    val addKotlinLabsDefinitions: Boolean,
    val foreignSchemas: List<ForeignSchema>,
)

private fun ForeignSchema.asNonPrefixedImport(): LinkedSchema {
  return LinkedSchema(this, definitions, definitions.map { (it as GQLNamed).definitionName() }.associateBy { it }, null)
}

private class LinkedDefinition<T : GQLDefinition>(val definition: T, val linkedSchema: LinkedSchema)

internal fun validateSchema(definitions: List<GQLDefinition>, options: SchemaValidationOptions): GQLResult<Schema> {
  val issues = mutableListOf<Issue>()

  /*
   * Make sure we have a full schema.
   * This can lead to issues if the Apollo Kotlin built-in definitions do not match the schema ones.
   * In those cases, some operations might be considered valid while they are in fact not supported
   * by the server.
   */
  val fullDefinitions = definitions.withBuiltinDefinitions()

  /**
   * TODO: this should probably be done after merging so that we can handle @link on the schema definition itself.
   */
  val linkedSchemas = definitions.filterIsInstance<GQLSchemaExtension>().getLinkedSchemas(issues, options.foreignSchemas).toMutableList()

  if (options.addKotlinLabsDefinitions && linkedSchemas.none { it.foreignSchema.name == "kotlin_labs" }) {
    linkedSchemas.add(autoLinkedKotlinLabsForeignSchema.asNonPrefixedImport())
  }

  val typeDefinitions = mutableMapOf<String, GQLTypeDefinition>()
  val directiveDefinitions = mutableMapOf<String, GQLDirectiveDefinition>()
  val typeSystemExtensions = mutableListOf<GQLTypeSystemExtension>()
  var schemaDefinition: GQLSchemaDefinition? = null
  val reportedDefinitions = mutableSetOf<GQLDefinition>()

  /*
   * Deduplicate the user input
   */
  fullDefinitions.forEach { definition ->
    when (definition) {
      is GQLTypeDefinition -> {
        val existing = typeDefinitions.get(definition.name)
        if (existing != null) {
          if (!reportedDefinitions.contains(existing)) {
            issues.add(OtherValidationIssue("Conflicting definition '${existing.name}'", existing.sourceLocation))
            reportedDefinitions.add(existing)
          }
          issues.add(OtherValidationIssue("Conflicting definition '${definition.name}'", definition.sourceLocation))
        } else {
          typeDefinitions.put(definition.name, definition)
        }
      }

      is GQLDirectiveDefinition -> {
        val existing = directiveDefinitions.get(definition.name)
        if (existing != null) {
          if (!reportedDefinitions.contains(existing)) {
            issues.add(OtherValidationIssue("Conflicting definition '${existing.definitionName()}'", existing.sourceLocation))
            reportedDefinitions.add(existing)
          }
          issues.add(OtherValidationIssue("Conflicting definition '${definition.definitionName()}'", definition.sourceLocation))
        } else {
          directiveDefinitions.put(definition.name, definition)
        }
      }

      is GQLSchemaDefinition -> {
        val existing = schemaDefinition
        if (existing != null) {
          if (!reportedDefinitions.contains(existing)) {
            issues.add(OtherValidationIssue("Conflicting schema definition", existing.sourceLocation))
            reportedDefinitions.add(existing)
          }
          issues.add(OtherValidationIssue("Conflicting schema definition", definition.sourceLocation))
        } else {
          schemaDefinition = definition
        }
      }

      is GQLTypeSystemExtension -> {
        typeSystemExtensions.add(definition)
      }

      else -> {
        issues.add(OtherValidationIssue("Unsupported definition. Schemas should only contain type system definitions or extensions.", definition.sourceLocation))
      }
    }
  }

  /*
   * Build a schema definition if not there already
   */
  if (schemaDefinition == null) {
    schemaDefinition = syntheticSchemaDefinition(typeDefinitions)
    if (schemaDefinition == null) {
      issues.add(NoQueryType("No schema definition and no query type found", null))
      return GQLResult(null, issues)
    }
  }

  /*
   * Deduplicate the imported definitions.
   * This is based on definition name + foreign schema name + foreign schema version.
   * This means that it's an error to import the same definition from 2 different foreign schemas, even if they are technically compatible.
   */
  val importedTypeDefinitions = mutableMapOf<String, LinkedDefinition<GQLTypeDefinition>>()
  val importedDirectiveDefinitions = mutableMapOf<String, LinkedDefinition<GQLDirectiveDefinition>>()
  reportedDefinitions.clear()
  linkedSchemas.forEach { import ->
    import.renamedDefinitions.forEach { definition ->
      when (definition) {
        is GQLTypeDefinition -> {
          val existing = importedTypeDefinitions.get(definition.name)
          if (existing != null && existing.linkedSchema.foreignSchema.nameWithVersion != import.foreignSchema.nameWithVersion) {
            if (!reportedDefinitions.contains(existing.definition)) {
              issues.add(OtherValidationIssue("Conflicting linked definition '${existing.definition.name}' (from foreign schema '${existing.linkedSchema.foreignSchema.nameWithVersion}')", null))
              reportedDefinitions.add(existing.definition)
            }
            issues.add(OtherValidationIssue("Conflicting linked definition '${definition.name}' (from foreign schema '${import.foreignSchema.nameWithVersion}')", null))
          } else {
            importedTypeDefinitions.put(definition.name, LinkedDefinition(definition, import))
          }
        }

        is GQLDirectiveDefinition -> {
          val existing = importedDirectiveDefinitions.get(definition.name)
          if (existing != null && existing.linkedSchema.foreignSchema.nameWithVersion != import.foreignSchema.nameWithVersion) {
            if (!reportedDefinitions.contains(existing.definition)) {
              issues.add(OtherValidationIssue("Conflicting linked definition '${existing.linkedSchema.foreignSchema.nameWithVersion}/${existing.definition.definitionName()}'", null))
              reportedDefinitions.add(existing.definition)
            }
            issues.add(OtherValidationIssue("Conflicting linked definition '${import.foreignSchema.nameWithVersion}/${definition.definitionName()}'", null))
          } else {
            importedDirectiveDefinitions.put(definition.name, LinkedDefinition(definition, import))
          }
        }

        else -> {
          issues.add(OtherValidationIssue("Unsupported linked definition. Foreign schemas should only contain type system definitions.", definition.sourceLocation))
        }
      }
    }
  }

  /*
   * Merge the linked definitions
   */
  importedTypeDefinitions.forEach { entry ->
    val existing = typeDefinitions.get(entry.key)
    if (existing != null) {
      issues.add(OtherValidationIssue("Linked definition '${entry.value.linkedSchema.foreignSchema.nameWithVersion}/${entry.value.definition.name}' is conflicting with an existing definition", existing.sourceLocation))
    } else {
      typeDefinitions.put(entry.key, entry.value.definition)
    }
  }
  importedDirectiveDefinitions.forEach { entry ->
    val existing = directiveDefinitions.get(entry.key)
    if (existing != null) {
      if (entry.value.linkedSchema.foreignSchema == autoLinkedKotlinLabsForeignSchema) {
        /*
         * This may be an auto-linked definition.
         * For compatibility reasons, it takes precedence over the user-provided ones.
         * In the future, we should ask the user to explicitly import the definitions
         * and rename them so that they do not clash.
         * TODO v5: remove this branch
         */
        issues.add(DirectiveRedefinition(entry.key, existing.sourceLocation, existing.sourceLocation))
        directiveDefinitions.put(entry.key, entry.value.definition)
      } else if (entry.value.linkedSchema.foreignSchema.name == Schema.LINK) {
        /*
         * Since we don't support renaming @link just yet, there's no good resolution if the directive
         * is already defined in the API schema. Just ignore the schema one in this case
         */
        issues.add(DirectiveRedefinition(entry.key, existing.sourceLocation, existing.sourceLocation))
        directiveDefinitions.put(entry.key, entry.value.definition)
      } else {
        issues.add(OtherValidationIssue("Definition is conflicting with linked definition '${entry.value.linkedSchema.foreignSchema.nameWithVersion}/${entry.value.definition.definitionName()}'", existing.sourceLocation))
      }
    } else {
      directiveDefinitions.put(entry.key, entry.value.definition)
    }
  }

  val foreignNames = linkedSchemas.flatMap {
    it.newNames.entries
  }.associateBy(keySelector = { it.value }, valueTransform = { it.key })

  /*
   * For definitions that we have special processing for, verify that they match
   * what we expect to provide a good error message.
   *
   * If not, we could get crashes in the compiler because it is expecting
   * certain argument names. Or a directive could be ignored, which would be surprising.
   *
   * Note: server implementations may ignore what checked directives we have and get stuck
   * here with an error or worse, trigger undesired behaviour without realizing it. If that
   * were to happen, we could add an "ignore" directive to ignore them. Or only do special
   * processing for the directives that are defined on the clients (which would be quite
   * hard to do at the moment because we do not distinguish server vs client `.graphqls`
   * files).
   *
   * TODO: should this be done after merging?
   */
  listOf(
      oneOfDefinitionsStr,
      deferDefinitionsStr,
      nonNullDefinitionStr,
      kotlinLabsDefinitions_0_4,
      compilerOptions_0_0,
      compilerOptions_0_1_additions,
      nullabilityDefinitionsStr,
      disableErrorPropagationStr
  ).flatMap {
    it.parseAsGQLDocument().getOrThrow().definitions
  }
      .forEach { expected ->
        val existing = when (expected) {
          is GQLTypeDefinition -> typeDefinitions.get(expected.name)
          is GQLDirectiveDefinition -> directiveDefinitions.get(expected.name)
          else -> error("")// should never happen
        }
        if (existing != null && !foreignNames.containsKey(expected.definitionName()) && !existing.semanticEquals(expected)) {
          /*
           * For non-linked definitions, check that the definitions match 1:1.
           * We do not check linked definitions because:
           * - we know we support them by construction.
           * - someone may rename argument types, which makes validation much harder. One example is importing `@catch` but not
           * `@catchTo`.
           */
          issues.add(IncompatibleDefinition(expected.name, expected.toSemanticSdl(), existing.sourceLocation))
        }
      }

  /**
   * I'm not 100% clear on the order of validations, here I'm merging the extensions first thing.
   *
   * It seems more natural. Two examples:
   * - If we were one day to validate that objects implement all interfaces fields for an example, this would have to be
   * done post merging (because extensions may add fields to interfaces).
   * - Same for validated repeated directives.
   *
   * Moving forward, extensions merging should probably be done first thing as a separate step, before any validation and/or linking of foreign schemas.
   */
  val dedupedDefinitions = listOfNotNull(schemaDefinition) + directiveDefinitions.values + typeDefinitions.values
  val mergedDefinitions = ExtensionsMerger(dedupedDefinitions + typeSystemExtensions, MergeOptions(false, true)).merge().getOrThrow()

  val mergedScope = DefaultValidationScope(
      typeDefinitions = mergedDefinitions.filterIsInstance<GQLTypeDefinition>().associateBy { it.name },
      directiveDefinitions = mergedDefinitions.filterIsInstance<GQLDirectiveDefinition>().associateBy { it.name },
      issues = issues,
      foreignNames = foreignNames,
  )
  mergedScope.validateNoIntrospectionNames()

  // The cast never fails because we enforce a single schema definition above.
  var mergedSchemaDefinition = mergedDefinitions.single { it is GQLSchemaDefinition } as GQLSchemaDefinition

  mergedSchemaDefinition = mergedSchemaDefinition.copy(directives = mergedSchemaDefinition.directives.filter { it.name != Schema.LINK })
  mergedScope.validateSchemaDefinition(mergedSchemaDefinition)
  mergedScope.validateInterfaces()
  mergedScope.validateObjects()
  mergedScope.validateUnions()
  mergedScope.validateInputObjects()
  mergedScope.validateScalars()
  mergedScope.validateDirectiveDefinitions()

  val keyFields = mergedScope.validateAndComputeKeyFields()
  val connectionTypes = mergedScope.computeConnectionTypes()

  return GQLResult(
      Schema(
          definitions = mergedDefinitions,
          keyFields = keyFields,
          foreignNames = foreignNames,
          directivesToStrip = linkedSchemas.flatMap { it.foreignSchema.directivesToStrip },
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

  return GQLSchemaDefinition(description = null, directives = emptyList(), rootOperationTypeDefinitions = operationTypeDefinitions
  )
}

/**
 * @param foreignSchema the [ForeignSchema] being imported.
 * @param renamedDefinitions [ForeignSchema.definitions] renamed according to the import.
 * By default, the new names are `${foreignSchemaName}__${definitionName}`
 * @param newNames a mapping from the initial foreign name to the new name.
 */
private class LinkedSchema(
    val foreignSchema: ForeignSchema,
    val renamedDefinitions: List<GQLDefinition>,
    val newNames: Map<DefinitionName, DefinitionName>,
    val foreignSchemaImportLocation: SourceLocation?,
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
private fun List<GQLSchemaExtension>.getLinkedSchemas(
    issues: MutableList<Issue>,
    foreignSchemas: List<ForeignSchema>,
): List<LinkedSchema> {
  val schemaExtensions = this

  val linkedSchemas = mutableListOf<LinkedSchema>()
  val linkLinkedSchema = ForeignSchema("link", "v1.0", linkDefinitions()).asNonPrefixedImport()
  schemaExtensions.forEach { schemaExtension ->
    schemaExtension.directives.forEach eachDirective@{ gqlDirective ->
      if (gqlDirective.name == Schema.LINK) {
        if (!linkedSchemas.contains(linkLinkedSchema)) {
          linkedSchemas.add(linkLinkedSchema)
        }

        /**
         * Validate `@link` using a very minimal schema.
         * This ensures we can safely cast the arguments below
         */
        val minimalSchema = builtinDefinitions() + linkLinkedSchema.foreignSchema.definitions
        val scope = DefaultValidationScope(
            minimalSchema.filterIsInstance<GQLTypeDefinition>().associateBy { it.name },
            minimalSchema.filterIsInstance<GQLDirectiveDefinition>().associateBy { it.name },
        )
        scope.validateDirectivesInConstContext(listOf(gqlDirective), schemaExtension)

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

        val importArgumentValue = (arguments.firstOrNull { it.name == "import" }?.value as GQLListValue?)?.values
        val imports = importArgumentValue.orEmpty().parseImport(issues)

        val foreignSchema = foreignSchemas.firstOrNull { it.name == foreignName && it.version == foreignVersion }

        if (foreignSchema != null) {
          /*
           * Check that the mappings refer to existing definitions
           */
          imports.forEach { import ->
            val key = import.key
            val linkedDefinition = foreignSchema.definitions.firstOrNull { it is GQLNamed && it.definitionName() == key }
            if (linkedDefinition == null) {
              issues.add(OtherValidationIssue("Apollo: unknown definition '$key'", gqlDirective.sourceLocation))
            }
          }

          val (definitions, renames) = foreignSchema.definitions.rename(imports, prefix)
          linkedSchemas.add(
              LinkedSchema(
                  foreignSchema = foreignSchema,
                  renamedDefinitions = definitions,
                  newNames = renames,
                  foreignSchemaImportLocation = gqlDirective.sourceLocation
              )
          )
        } else {
          issues.add(
              OtherValidationIssue(
                  message = "Apollo: unknown foreign schema '$foreignName/$foreignVersion'",
                  sourceLocation = gqlDirective.sourceLocation
              )
          )
        }
      }
    }
  }

  return linkedSchemas
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
private fun List<GQLValue>.parseImport(issues: MutableList<Issue>): Map<DefinitionName, DefinitionName> {
  return mapNotNull {
    when (it) {
      is GQLStringValue -> {
        // Simple case: import the definition without renaming
        it.value to it.value
      }

      is GQLObjectValue -> {
        if (it.fields.size != 2) {
          issues.add(OtherValidationIssue("Too many fields in 'import' argument", it.sourceLocation))
          return@mapNotNull null
        }

        val name = (it.fields.firstOrNull { it.name == "name" }?.value as? GQLStringValue)?.value
        if (name == null) {
          issues.add(OtherValidationIssue("import 'name' argument is either missing or not a string", it.sourceLocation))
        }
        val asValue = it.fields.firstOrNull { it.name == "as" }?.value
        val as2 = (asValue as? GQLStringValue)?.value
        if (as2 == null) {
          issues.add(OtherValidationIssue("import 'as' argument is either missing or not a string", it.sourceLocation))
        }
        if (name == null || as2 == null) {
          return@mapNotNull null
        }
        if (name.startsWith('@') && !as2.startsWith('@')) {
          issues.add(OtherValidationIssue("Apollo: 'as' argument value must start with '@' when importing directives", asValue.sourceLocation))
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

/**
 * The name of a type definition or, for directives, the name prefixed with '@'.
 *
 * This is to avoid directives possibly clashing with types (very unlikely but still).
 */
private typealias DefinitionName = String

private fun List<GQLDefinition>.rename(
    imports: Map<DefinitionName, DefinitionName>,
    prefix: String,
): Pair<List<GQLDefinition>, Map<DefinitionName, DefinitionName>> {
  val renames = mutableMapOf<DefinitionName, DefinitionName>()
  fun GQLNamed.newName(): String {
    val definitionName = definitionName()

    val isDirective = this is GQLDirectiveDefinition
    var rename = imports.get(definitionName)
    if (rename == null) {
      // default rename if nothing is explicitly imported
      val at = if (isDirective) {
        "@"
      } else {
        ""
      }
      rename = "${at}${prefix}__${name}"
    }

    renames[definitionName] = rename
    return if (isDirective) {
      /*
       * Do not add an extra '@' in the directive name.
       */
      rename.substring(1)
    } else {
      rename
    }
  }

  val definitions = this.map { gqlDefinition ->
    /*
     * First pass: rename the types
     */
    gqlDefinition.transform2 { gqlNode ->
      when (gqlNode) {
        is GQLScalarTypeDefinition -> gqlNode.copy(name = gqlNode.newName())
        is GQLObjectTypeDefinition -> gqlNode.copy(name = gqlNode.newName())
        is GQLEnumTypeDefinition -> gqlNode.copy(name = gqlNode.newName())
        is GQLInterfaceTypeDefinition -> gqlNode.copy(name = gqlNode.newName())
        is GQLUnionTypeDefinition -> gqlNode.copy(name = gqlNode.newName())
        is GQLInputObjectTypeDefinition -> gqlNode.copy(name = gqlNode.newName())
        is GQLDirectiveDefinition -> gqlNode.copy(name = gqlNode.newName())
        else -> gqlNode
      }
    } as GQLDefinition
  }.map { gqlDefinition ->
    /*
     * Second pass: rename the references using the previously computed renames.
     */
    gqlDefinition.transform2 { gqlNode ->
      if (gqlNode is GQLNamedType) {
        gqlNode.copy(name = renames[gqlNode.name] ?: gqlNode.name)
      } else {
        gqlNode
      }
    } as GQLDefinition
  }

  return definitions to renames
}

/**
 * Because types and directive may share the same name, they are disambiguated
 */
private fun GQLNamed.definitionName(): String {
  return when (this) {
    is GQLDirectiveDefinition -> {
      "@${name}"
    }

    else -> {
      name
    }
  }
}

internal fun ValidationScope.validateSchemaDefinition(schemaDefinition: GQLSchemaDefinition) {
  validateDirectivesInConstContext(schemaDefinition.directives, schemaDefinition)

  schemaDefinition.rootOperationTypeDefinitions.forEach {
    val typeDefinition = typeDefinitions[it.namedType]
    if (typeDefinition == null) {
      registerIssue("Schema defines `${it.namedType}` as root for `${it.namedType}` but `${it.namedType}` is not defined", sourceLocation = it.sourceLocation
      )
    }
  }

  validateCatch(schemaDefinition)
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

    validateDirectivesInConstContext(i.directives, i)

    i.fields.forEach { fieldDefinition ->
      validateField(fieldDefinition)
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

    validateDirectivesInConstContext(o.directives, o)

    o.fields.forEach { fieldDefinition ->
      validateField(fieldDefinition)
    }
  }
}

private fun ValidationScope.validateField(fieldDefinition: GQLFieldDefinition) {
  validateDirectivesInConstContext(fieldDefinition.directives, fieldDefinition)

  fieldDefinition.arguments.forEach {
    if (it.defaultValue != null) {
      validateAndCoerceValue(it.defaultValue, it.type, false, false) {
        issues.add(it.constContextError())
      }
    }
  }
}

private fun ValidationScope.validateUnions() {
  typeDefinitions.values.filterIsInstance<GQLUnionTypeDefinition>().forEach { u ->
    validateDirectivesInConstContext(u.directives, u)
  }
}

private fun ValidationScope.validateCatch(schemaDefinition: GQLSchemaDefinition) {
  val hasCatchDefinition = directiveDefinitions.any {
    originalDirectiveName(it.key) == Schema.CATCH
  }

  if (!hasCatchDefinition) {
    return
  }

  val catches = schemaDefinition.directives.filter {
    originalDirectiveName(it.name) == Schema.CATCH_BY_DEFAULT
  }

  if (catches.isEmpty()) {
    issues.add(OtherValidationIssue(message = "Schemas that include nullability directives must opt-in a default CatchTo. Use `extend schema @catchByDefault(to: \$to)`", sourceLocation = schemaDefinition.sourceLocation
    )
    )
    return
  }
}
private fun ValidationScope.validateDirectiveDefinitions() {
  directiveDefinitions.values.forEach {
    it.arguments.forEach {
      if (it.defaultValue != null) {
        validateAndCoerceValue(it.defaultValue, it.type, false, false) {
          issues.add(it.constContextError())
        }
      }
    }
  }
}

private fun ValidationScope.validateScalars() {
  typeDefinitions.values.filterIsInstance<GQLScalarTypeDefinition>().forEach { scalarTypeDefinition ->
    validateDirectivesInConstContext(scalarTypeDefinition.directives, scalarTypeDefinition)
    var hasMap = false
    var hasMapTo = false
    scalarTypeDefinition.directives.forEach {
      when (originalDirectiveName(it.name)) {
        Schema.MAP_TO -> hasMapTo = true
        Schema.MAP -> hasMap = true
      }
    }
    if (hasMap && hasMapTo) {
      issues.add(OtherValidationIssue(
          message = "Only one of @map and @mapTo can be added to a scalar.",
          sourceLocation = scalarTypeDefinition.sourceLocation
      )
      )
    }
  }
}

private fun ValidationScope.validateInputObjects() {
  val traversalState = TraversalState()
  val defaultValueTraversalState = DefaultValueTraversalState()
  typeDefinitions.values.filterIsInstance<GQLInputObjectTypeDefinition>().forEach { o ->
    if (o.inputFields.isEmpty()) {
      registerIssue("Input object must specify one or more input fields", o.sourceLocation)
    }

    validateDirectivesInConstContext(o.directives, o)
    validateInputFieldCycles(o, traversalState)
    validateInputObjectDefaultValue(o, defaultValueTraversalState)

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

private class TraversalState {
  val visitedTypes = mutableSetOf<String>()
  val fieldPath = mutableListOf<Pair<String, SourceLocation?>>()
  val fieldPathIndexByTypeName = mutableMapOf<String, Int>()
}

private class DefaultValueTraversalState {
  val visitedFields = mutableSetOf<String>()
  val fieldPath = mutableListOf<Pair<String, SourceLocation?>>()
  val fieldPathIndex = mutableMapOf<String, Int>()
}


private fun ValidationScope.validateInputFieldCycles(inputObjectTypeDefinition: GQLInputObjectTypeDefinition, state: TraversalState) {
  if (state.visitedTypes.contains(inputObjectTypeDefinition.name)) {
    return
  }
  state.visitedTypes.add(inputObjectTypeDefinition.name)

  state.fieldPathIndexByTypeName[inputObjectTypeDefinition.name] = state.fieldPath.size

  inputObjectTypeDefinition.inputFields.forEach {
    val type = it.type
    if (type is GQLNonNullType && type.type is GQLNamedType) {
      val fieldType = typeDefinitions.get(type.type.name)
      if (fieldType is GQLInputObjectTypeDefinition) {
        val cycleIndex = state.fieldPathIndexByTypeName.get(fieldType.name)

        state.fieldPath.add("${fieldType.name}.${it.name}" to it.sourceLocation)

        if (cycleIndex == null) {
          validateInputFieldCycles(fieldType, state)
        } else {
          val cyclePath = state.fieldPath.subList(cycleIndex, state.fieldPath.size)

          cyclePath.forEach {
            issues.add(
                OtherValidationIssue(
                    buildString {
                      append("Invalid circular reference. The Input Object '${fieldType.name}' references itself ")
                      if (cyclePath.size > 1) {
                        append("via the non-null fields: ")
                      } else {
                        append("in the non-null field ")
                      }
                      append(cyclePath.map { it.first }.joinToString(", "))
                    },
                    it.second
                )
            )
          }
        }

        state.fieldPath.removeLast()
      }
    }
  }

  state.fieldPathIndexByTypeName.remove(inputObjectTypeDefinition.name)
}
private fun ValidationScope.validateInputObjectDefaultValue(
    inputObjectTypeDefinition: GQLInputObjectTypeDefinition,
    state: DefaultValueTraversalState
) {
  validateInputObjectDefaultValue(inputObjectTypeDefinition, GQLObjectValue(null,emptyList()), state)
}
private fun ValidationScope.validateInputObjectDefaultValue(
    inputObjectTypeDefinition: GQLInputObjectTypeDefinition,
    defaultValue: GQLValue,
    state: DefaultValueTraversalState
) {
  if (defaultValue is GQLListValue) {
    defaultValue.values.forEach {
      validateInputObjectDefaultValue(inputObjectTypeDefinition, it, state)
    }
  } else if (defaultValue is GQLObjectValue) {
    inputObjectTypeDefinition.inputFields.forEach { inputField ->
      val rawType = inputField.type.rawType()
      val typeDefinition = typeDefinitions.get(rawType.name)
      if (typeDefinition !is GQLInputObjectTypeDefinition) {
        return
      }
      val fieldDefaultValue = defaultValue.fields.firstOrNull { it.name == inputField.name}
      if (fieldDefaultValue != null) {
        validateInputObjectDefaultValue(typeDefinition, fieldDefaultValue.value, state)
      } else {
        validateInputFieldDefaultValue(inputField, "${inputObjectTypeDefinition.name}.${inputField.name}", defaultValue, typeDefinition, state)
      }
    }
  }
}

private fun ValidationScope.validateInputFieldDefaultValue(
    inputFieldDefinition: GQLInputValueDefinition,
    fieldStr: String,
    defaultValue: GQLObjectValue,
    typeDefinition: GQLInputObjectTypeDefinition,
    state: DefaultValueTraversalState
) {
  val fieldDefaultValue = inputFieldDefinition.defaultValue
  if (fieldDefaultValue == null) {
    return
  }

  val cycleIndex = state.fieldPathIndex[fieldStr]
  if (cycleIndex != null) {
    val cyclePath = state.fieldPath.subList(cycleIndex, state.fieldPath.size)
    cyclePath.forEach {
      issues.add(
          OtherValidationIssue(
              buildString {
                append("Invalid circular reference. The default value of Input Object field $fieldStr references itself")
                if (cyclePath.size > 1) {
                  append(" via the default values of: ")
                  append(cyclePath.map { it.first }.joinToString(", "))
                }
                append('.')
              },
              it.second
          )
      )
    }
  }
  if (state.visitedFields.contains(fieldStr)) {
    return
  }

  state.visitedFields.add(fieldStr)
  state.fieldPathIndex.put(fieldStr, state.fieldPath.size)
  state.fieldPath.add(fieldStr to fieldDefaultValue.sourceLocation)

  validateInputObjectDefaultValue(typeDefinition, fieldDefaultValue, state)

  state.fieldPathIndex.remove(fieldStr)
  state.fieldPath.removeLast()
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

