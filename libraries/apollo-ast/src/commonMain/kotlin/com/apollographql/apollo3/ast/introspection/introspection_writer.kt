@file:JvmMultifileClass
@file:JvmName("Introspection")
@file:Suppress("PropertyName")

package com.apollographql.apollo.ast.introspection

import com.apollographql.apollo.ast.ConversionException
import com.apollographql.apollo.ast.GQLDirectiveDefinition
import com.apollographql.apollo.ast.GQLDocument
import com.apollographql.apollo.ast.GQLEnumTypeDefinition
import com.apollographql.apollo.ast.GQLEnumValueDefinition
import com.apollographql.apollo.ast.GQLFieldDefinition
import com.apollographql.apollo.ast.GQLInputObjectTypeDefinition
import com.apollographql.apollo.ast.GQLInputValueDefinition
import com.apollographql.apollo.ast.GQLInterfaceTypeDefinition
import com.apollographql.apollo.ast.GQLListType
import com.apollographql.apollo.ast.GQLNamedType
import com.apollographql.apollo.ast.GQLNonNullType
import com.apollographql.apollo.ast.GQLObjectTypeDefinition
import com.apollographql.apollo.ast.GQLOperationTypeDefinition
import com.apollographql.apollo.ast.GQLScalarTypeDefinition
import com.apollographql.apollo.ast.GQLSchemaDefinition
import com.apollographql.apollo.ast.GQLType
import com.apollographql.apollo.ast.GQLTypeDefinition
import com.apollographql.apollo.ast.GQLUnionTypeDefinition
import com.apollographql.apollo.ast.findDeprecationReason
import com.apollographql.apollo.ast.findOneOf
import com.apollographql.apollo.ast.findSpecifiedBy
import com.apollographql.apollo.ast.toUtf8
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.jvm.JvmMultifileClass
import kotlin.jvm.JvmName

/**
 * The Json Model matching the `__Schema` [GraphQL type](https://spec.graphql.org/draft/#sec-The-__Schema-Type)
 *
 * It matches the draft version of the spec. Some fields did not exist in earlier versions
 * like `__InputValue.isDeprecated` for an example.
 *
 * The models match with the spec types except for Type that is used recursively, and therefore
 * we have [WTypeFull], [WTypeRef] and [WTypeRoot]
 */
@Serializable
internal class WSchema(
    internal val description: String?,
    internal val types: List<WTypeFull>,
    internal val queryType: WTypeRoot,
    internal val mutationType: WTypeRoot?,
    internal val subscriptionType: WTypeRoot?,
    internal val directives: List<WDirective>,
)

@Serializable
internal class WTypeRoot(val name: String)

@Serializable
internal class WTypeFull(
    val kind: WTypeKind,
    // name is nullable in the spec but when queried from __schema.types, it should never be
    val name: String,
    val description: String?,
    val fields: List<WField>?,
    val interfaces: List<WTypeRef>?,
    val possibleTypes: List<WTypeRef>?,
    val enumValues: List<WEnumValue>?,
    val inputFields: List<WInputValue>?,
    val specifiedByURL: String?,
    val isOneOf: Boolean?,
)

@Serializable
internal class WTypeRef(
    val kind: WTypeKind,
    val name: String?,
    val ofType: WTypeRef?,
)

internal enum class WTypeKind {
  SCALAR,
  OBJECT,
  INTERFACE,
  UNION,
  ENUM,
  INPUT_OBJECT,
  LIST,
  NON_NULL,
}

@Serializable
internal class WField(
    val name: String,
    val description: String?,
    val args: List<WInputValue>,
    val type: WTypeRef,
    val isDeprecated: Boolean,
    val deprecationReason: String?,
)

@Serializable
internal class WInputValue(
    val name: String,
    val description: String?,
    val type: WTypeRef,
    val defaultValue: String?,
    val isDeprecated: Boolean,
    val deprecationReason: String?,
)

@Serializable
internal class WEnumValue(
    val name: String,
    val description: String?,
    val isDeprecated: Boolean,
    val deprecationReason: String?,
)

@Serializable
internal class WDirective(
    val name: String,
    val description: String?,
    val locations: List<String>,
    val args: List<WInputValue>,
    val isRepeatable: Boolean,
)

@Serializable
internal class WData(val __schema: WSchema)

internal class IntrospectionSchemaBuilder(document: GQLDocument) {
  private val typeDefinitions: Map<String, GQLTypeDefinition>
  private val directiveDefinitions: List<GQLDirectiveDefinition>
  private val schemaDefinition: GQLSchemaDefinition

  init {
    val types = mutableMapOf<String, GQLTypeDefinition>()
    val directives = mutableListOf<GQLDirectiveDefinition>()
    var schema: GQLSchemaDefinition? = null

    document.definitions.forEach {
      when (it) {
        is GQLTypeDefinition -> {
          types.put(it.name, it)
        }

        is GQLDirectiveDefinition -> {
          directives.add(it)
        }

        is GQLSchemaDefinition -> {
          schema = it
        }

        else -> {
          throw ConversionException("Unsupported definition ${it::class.simpleName} apply type extensions before converting to introspection")
        }
      }
    }

    schemaDefinition = schema ?: defaultSchemaDefinition(types)
    typeDefinitions = types
    directiveDefinitions = directives
  }

  fun toIntrospectionSchema(): ApolloIntrospectionSchema {
    return ApolloIntrospectionSchema(
        WSchema(
            queryType = WTypeRoot(schemaDefinition.queryType()),
            mutationType = schemaDefinition.mutationType()?.let { WTypeRoot(it) },
            subscriptionType = schemaDefinition.subscriptionType()?.let { WTypeRoot(it) },
            types = typeDefinitions.values.map {
              when (it) {
                is GQLObjectTypeDefinition -> it.toIntrospectionType()
                is GQLInputObjectTypeDefinition -> it.toIntrospectionType()
                is GQLInterfaceTypeDefinition -> it.toIntrospectionType()
                is GQLScalarTypeDefinition -> it.toIntrospectionType()
                is GQLEnumTypeDefinition -> it.toIntrospectionType()
                is GQLUnionTypeDefinition -> it.toIntrospectionType()
              }
            },
            directives = directiveDefinitions.map { it.toIntrospectionDirective() },
            description = schemaDefinition.description
        )
    )
  }

  private fun GQLObjectTypeDefinition.toIntrospectionType(): WTypeFull {
    return WTypeFull(
        kind = WTypeKind.OBJECT,
        name = name,
        description = description,
        fields = fields.map { it.toIntrospectionField() },
        interfaces = implementsInterfaces.map { WTypeRef(kind = WTypeKind.INTERFACE, name = it, ofType = null) },
        possibleTypes = null,
        enumValues = null,
        inputFields = null,
        specifiedByURL = null,
        isOneOf = null,
    )
  }

  private fun GQLFieldDefinition.toIntrospectionField(): WField {
    val deprecationReason = directives.findDeprecationReason()
    return WField(
        name = name,
        description = description,
        isDeprecated = deprecationReason != null,
        deprecationReason = deprecationReason,
        type = type.toIntrospectionType(),
        args = arguments.map { it.toIntrospectionInputValue() }
    )
  }

  private fun GQLInputValueDefinition.toIntrospectionInputValue(): WInputValue {
    val deprecationReason = directives.findDeprecationReason()

    return WInputValue(
        name = name,
        description = description,
        isDeprecated = deprecationReason != null,
        deprecationReason = deprecationReason,
        type = type.toIntrospectionType(),
        defaultValue = defaultValue?.toUtf8(indent = "")
    )
  }

  private fun GQLInputObjectTypeDefinition.toIntrospectionType(): WTypeFull {
    return WTypeFull(
        kind = WTypeKind.INPUT_OBJECT,
        name = name,
        description = description,
        inputFields = inputFields.map { it.toIntrospectionInputValue() },
        fields = null,
        interfaces = null,
        possibleTypes = null,
        enumValues = null,
        specifiedByURL = null,
        isOneOf = directives.findOneOf(),
    )
  }


  private fun GQLInterfaceTypeDefinition.toIntrospectionType(): WTypeFull {
    return WTypeFull(
        kind = WTypeKind.INTERFACE,
        name = name,
        description = description,
        fields = fields.map { it.toIntrospectionField() },
        possibleTypes = typeDefinitions.values
            .filter { typeDefinition ->
              typeDefinition is GQLObjectTypeDefinition && typeDefinition.implementsInterfaces.contains(name)
            }
            .map { typeDefinition ->
              WTypeRef(
                  kind = WTypeKind.OBJECT,
                  name = typeDefinition.name,
                  ofType = null
              )
            },
        interfaces = implementsInterfaces.map { interfaceName ->
          WTypeRef(
              kind = WTypeKind.INTERFACE,
              name = interfaceName,
              ofType = null
          )
        },
        enumValues = null,
        inputFields = null,
        specifiedByURL = null,
        isOneOf = null,
    )
  }

  private fun GQLEnumTypeDefinition.toIntrospectionType(): WTypeFull {
    return WTypeFull(
        kind = WTypeKind.ENUM,
        name = name,
        description = description,
        enumValues = enumValues.map { it.toIntrospectionEnumValue() },
        fields = null,
        interfaces = null,
        inputFields = null,
        possibleTypes = null,
        specifiedByURL = null,
        isOneOf = null,
    )
  }

  private fun GQLEnumValueDefinition.toIntrospectionEnumValue(): WEnumValue {
    val deprecationReason = directives.findDeprecationReason()
    return WEnumValue(
        name = name,
        description = description,
        isDeprecated = deprecationReason != null,
        deprecationReason = deprecationReason
    )
  }

  private fun GQLScalarTypeDefinition.toIntrospectionType(): WTypeFull {
    return WTypeFull(
        kind = WTypeKind.SCALAR,
        name = this.name,
        description = this.description,
        fields = null,
        interfaces = null,
        possibleTypes = null,
        enumValues = null,
        inputFields = null,
        specifiedByURL = this.directives.findSpecifiedBy(),
        isOneOf = null,
    )
  }

  private fun GQLUnionTypeDefinition.toIntrospectionType(): WTypeFull {
    return WTypeFull(
        kind = WTypeKind.UNION,
        name = name,
        description = description,
        fields = null,
        possibleTypes = memberTypes.map { it.toIntrospectionType() },
        interfaces = null,
        enumValues = null,
        inputFields = null,
        specifiedByURL = null,
        isOneOf = null,
    )
  }

  private fun GQLDirectiveDefinition.toIntrospectionDirective() = WDirective(
      name = name,
      description = description,
      locations = locations.map { it.name },
      args = arguments.map { it.toIntrospectionInputValue() },
      isRepeatable = repeatable,
  )

  private fun GQLType.toIntrospectionType(): WTypeRef {
    return when (this) {
      is GQLNonNullType -> {
        WTypeRef(
            kind = WTypeKind.NON_NULL,
            name = null,
            ofType = type.toIntrospectionType()
        )
      }

      is GQLListType -> {
        WTypeRef(
            kind = WTypeKind.LIST,
            name = null,
            ofType = type.toIntrospectionType())
      }

      is GQLNamedType -> {
        WTypeRef(
            kind = typeDefinitions.get(name)?.schemaKind() ?: throw ConversionException("Cannot find type $name"),
            name = name,
            ofType = null
        )
      }
    }
  }
}

internal fun defaultSchemaDefinition(typeDefinitions: Map<String, GQLTypeDefinition>): GQLSchemaDefinition {
  return GQLSchemaDefinition(
      description = null,
      directives = emptyList(),
      rootOperationTypeDefinitions = listOfNotNull(
          typeDefinitions.getOperationTypeDefinition("Query", "query"),
          typeDefinitions.getOperationTypeDefinition("Mutation", "mutation"),
          typeDefinitions.getOperationTypeDefinition("Subscription", "subscription")
      )
  )
}

internal fun GQLSchemaDefinition.queryType(): String {
  return rootTypeFor("query") ?: throw ConversionException("Schema does not define a 'query' type")
}

internal fun GQLSchemaDefinition.mutationType(): String? {
  return rootTypeFor("mutation")
}

internal fun GQLSchemaDefinition.subscriptionType(): String? {
  return rootTypeFor("subscription")
}

internal fun GQLSchemaDefinition.rootTypeFor(operationType: String): String? {
  return rootOperationTypeDefinitions.firstOrNull {
    it.operationType == operationType
  }?.namedType
}


private fun Map<String, GQLTypeDefinition>.getOperationTypeDefinition(
    namedType: String,
    operationType: String,
): GQLOperationTypeDefinition? {
  if (!containsKey(namedType)) {
    return null
  }
  return GQLOperationTypeDefinition(
      operationType = operationType,
      namedType = namedType
  )
}

private fun GQLTypeDefinition.schemaKind() = when (this) {
  is GQLEnumTypeDefinition -> WTypeKind.ENUM
  is GQLUnionTypeDefinition -> WTypeKind.UNION
  is GQLObjectTypeDefinition -> WTypeKind.OBJECT
  is GQLInputObjectTypeDefinition -> WTypeKind.INPUT_OBJECT
  is GQLScalarTypeDefinition -> WTypeKind.SCALAR
  is GQLInterfaceTypeDefinition -> WTypeKind.INTERFACE
}

class ApolloIntrospectionSchema internal constructor(@Suppress("PropertyName") internal val __schema: WSchema)

fun ApolloIntrospectionSchema.toJson(): String {
  return Json.encodeToString(WData(__schema))
}

fun GQLDocument.toIntrospectionSchema(): ApolloIntrospectionSchema = IntrospectionSchemaBuilder(this).toIntrospectionSchema()
