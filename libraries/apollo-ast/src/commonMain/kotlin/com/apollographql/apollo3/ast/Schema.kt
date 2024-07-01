package com.apollographql.apollo.ast

import com.apollographql.apollo.annotations.ApolloExperimental
import com.apollographql.apollo.annotations.ApolloInternal

/**
 * A wrapper around a schema [GQLDocument] that ensures the [GQLDocument] is valid and caches
 * some extra information. In particular, [Schema]:
 * - always contain builtin definitions (SDL can omit them)
 * - always has a schema definition for easier lookup of root operation types
 * - has all type system extensions merged
 * - has some helper functions to retrieve a type by name and/or possible types
 * - caches [keyFields] for easier lookup during codegen
 * - remembers [foreignNames] to keep track of renamed definitions
 * - remembers [directivesToStrip] to keep track of client-only directives
 *
 * @param definitions a list of validated and merged definitions
 * @param keyFields a Map containing the key fields for each type
 * @param foreignNames a Map from a type system name -> its original name in the foreign schema.
 * To distinguish between directives and types, directive names must be prefixed by '@'
 * Example: "@kotlin_labs_nonnull" -> "@nonnull"
 * @param directivesToStrip directives to strip because they are coming from a foreign schema
 * Example: "kotlin_labs_nonnull"
 */
class Schema internal constructor(
    private val definitions: List<GQLDefinition>,
    private val keyFields: Map<String, Set<String>>,
    val foreignNames: Map<String, String>,
    private val directivesToStrip: List<String>,
    @ApolloInternal
    val connectionTypes: Set<String>,
) {
  @ApolloInternal
  val schemaDefinition: GQLSchemaDefinition? = definitions.filterIsInstance<GQLSchemaDefinition>().singleOrNull()

  val typeDefinitions: Map<String, GQLTypeDefinition> = definitions
      .filterIsInstance<GQLTypeDefinition>()
      .associateBy { it.name }

  val directiveDefinitions: Map<String, GQLDirectiveDefinition> = definitions
      .filterIsInstance<GQLDirectiveDefinition>()
      .associateBy { it.name }

  val errorAware: Boolean = directiveDefinitions.any {
    originalDirectiveName(it.key) == CATCH
  }

  val queryTypeDefinition: GQLTypeDefinition = rootOperationTypeDefinition("query", definitions)
      ?: throw SchemaValidationException("No query root type found")

  val mutationTypeDefinition: GQLTypeDefinition? = rootOperationTypeDefinition("mutation", definitions)

  val subscriptionTypeDefinition: GQLTypeDefinition? = rootOperationTypeDefinition("subscription", definitions)

  fun toGQLDocument(): GQLDocument = GQLDocument(
      definitions = definitions,
      sourceLocation = null
  )

  /**
   * @param name the current name of the directive (like "kotlin_labs__nonnull")
   *
   * @return the original directive name (like "nonnull")
   */
  fun originalDirectiveName(name: String): String {
    return foreignNames["@$name"]?.substring(1) ?: name
  }

  fun originalTypeName(name: String): String {
    return foreignNames[name] ?: name
  }

  fun rootTypeNameOrNullFor(operationType: String): String? {
    return rootOperationTypeDefinition(operationType, definitions)?.name
  }

  fun rootTypeNameFor(operationType: String): String {
    return rootTypeNameOrNullFor(operationType) ?: operationType.replaceFirstChar { it.uppercaseChar() }
  }

  fun typeDefinition(name: String): GQLTypeDefinition {
    return typeDefinitions[name]
        ?: throw SchemaValidationException("Cannot find type `$name`")
  }

  /**
   * returns all possible types:
   * - for an object, return this object
   * - for an interface, returns all objects implementing this interface (possibly transitively)
   * - for an union, returns all members
   *
   * TODO v4: It's unclear whether we need this for scalar and enums.
   */
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
   * Returns the [Schema] as a [Map] that can be easily serialized to Json
   */
  @ApolloInternal
  fun toMap(): Map<String, Any> {
    return mapOf(
        "sdl" to GQLDocument(definitions, sourceLocation = null).toSDL(),
        "keyFields" to keyFields.mapValues { it.value.toList().sorted() },
        "foreignNames" to foreignNames,
        "directivesToStrip" to directivesToStrip,
        "connectionTypes" to connectionTypes.toList().sorted(),
    )
  }

  /**
   * List all types (types, interfaces, unions) implemented by a given type (including itself)
   */
  fun implementedTypes(name: String): Set<String> {
    val typeDefinition = typeDefinition(name)
    return when (typeDefinition) {
      is GQLObjectTypeDefinition -> {
        val unions = typeDefinitions.values.filterIsInstance<GQLUnionTypeDefinition>().filter {
          it.memberTypes.map { it.name }.toSet().contains(typeDefinition.name)
        }.map { it.name }
        typeDefinition.implementsInterfaces.flatMap { implementedTypes(it) }.toSet() + name + unions
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
   * List all direct super types (interfaces, unions) implemented by a given object type
   */
  fun superTypes(objectTypeDefinition: GQLObjectTypeDefinition): Set<String> {
    val unions = typeDefinitions.values.filterIsInstance<GQLUnionTypeDefinition>().filter {
      it.memberTypes.map { it.name }.toSet().contains(objectTypeDefinition.name)
    }.map { it.name }
    return (objectTypeDefinition.implementsInterfaces + unions).toSet()
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
   */
  @ApolloInternal
  fun keyFields(name: String): Set<String> {
    return keyFields[name] ?: emptySet()
  }

  /**
   * return whether the given directive should be removed from operation documents before being sent to the server
   */
  @ApolloInternal
  fun shouldStrip(name: String): Boolean {
    return directivesToStrip.contains(name)
  }

  companion object {
    const val TYPE_POLICY = "typePolicy"
    const val FIELD_POLICY = "fieldPolicy"
    const val NONNULL = "nonnull"
    const val OPTIONAL = "optional"
    const val REQUIRES_OPT_IN = "requiresOptIn"
    const val TARGET_NAME = "targetName"

    @ApolloExperimental
    const val ONE_OF = "oneOf"
    @ApolloExperimental
    const val CATCH = "catch"
    @ApolloExperimental
    const val CATCH_BY_DEFAULT = "catchByDefault"
    @ApolloExperimental
    const val SEMANTIC_NON_NULL = "semanticNonNull"
    @ApolloExperimental
    const val SEMANTIC_NON_NULL_FIELD = "semanticNonNullField"
    @ApolloExperimental
    const val LINK = "link"

    const val FIELD_POLICY_FOR_FIELD = "forField"
    const val FIELD_POLICY_KEY_ARGS = "keyArgs"

    @ApolloExperimental
    const val FIELD_POLICY_PAGINATION_ARGS = "paginationArgs"

    /**
     * Parses the given [map] and creates a new [Schema].
     * The [map] must come from a previous call to [toMap] to make sure the schema is valid
     */
    @Suppress("UNCHECKED_CAST")
    @ApolloInternal
    fun fromMap(map: Map<String, Any>): Schema {
      return Schema(
          definitions = combineDefinitions((map["sdl"] as String).parseAsGQLDocument().getOrThrow().definitions, builtinDefinitions(), ConflictResolution.TakeLeft),
          keyFields = (map["keyFields"]!! as Map<String, Collection<String>>).mapValues { it.value.toSet() },
          foreignNames = map["foreignNames"]!! as Map<String, String>,
          directivesToStrip = map["directivesToStrip"]!! as List<String>,
          connectionTypes = (map["connectionTypes"]!! as List<String>).toSet(),
      )
    }

    internal fun rootOperationTypeDefinition(operationType: String, definitions: List<GQLDefinition>): GQLTypeDefinition? {
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
  }

}
