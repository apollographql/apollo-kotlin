package com.apollographql.apollo3.ast

import com.apollographql.apollo3.annotations.ApolloDeprecatedSince
import com.apollographql.apollo3.annotations.ApolloExperimental
import com.apollographql.apollo3.annotations.ApolloInternal
import okio.Buffer

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
) {
  /**
   * Creates a new Schema from a list of definition.
   * This doesn't support foreign schemas.
   *
   * See also [validateAsSchema] and [toSchema]
   */
  @Deprecated("Use validateAsSchema() to get a Schema")
  @ApolloDeprecatedSince(ApolloDeprecatedSince.Version.v3_3_1)
  constructor(definitions: List<GQLDefinition>) : this(
      definitions,
      emptyMap(),
      emptyMap(),
      emptyList()
  )

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
   * Returns the [Schema] as a [Map] that can be easily serialized to Json
   */
  @ApolloInternal
  fun toMap(): Map<String, Any> {
    return mapOf(
        "sdl" to GQLDocument(definitions, null).toUtf8(),
        "keyFields" to keyFields,
        "foreignNames" to foreignNames,
        "directivesToStrip" to directivesToStrip
    )
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
          definitions = Buffer().writeUtf8(map["sdl"] as String).parseAsGQLDocument().value!!.definitions,
          keyFields = (map["keyFields"]!! as Map<String, Collection<String>>).mapValues { it.value.toSet() },
          foreignNames = map["foreignNames"]!! as Map<String, String>,
          directivesToStrip = map["directivesToStrip"]!! as List<String>,
      )
    }
  }
}

