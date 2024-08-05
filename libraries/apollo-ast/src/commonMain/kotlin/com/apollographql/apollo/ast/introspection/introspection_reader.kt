@file:JvmMultifileClass
@file:JvmName("Introspection")

@file:Suppress("PropertyName")

package com.apollographql.apollo.ast.introspection

import com.apollographql.apollo.annotations.ApolloExperimental
import com.apollographql.apollo.annotations.ApolloInternal
import com.apollographql.apollo.ast.ConversionException
import com.apollographql.apollo.ast.GQLArgument
import com.apollographql.apollo.ast.GQLDirective
import com.apollographql.apollo.ast.GQLDirectiveDefinition
import com.apollographql.apollo.ast.GQLDirectiveLocation
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
import com.apollographql.apollo.ast.GQLStringValue
import com.apollographql.apollo.ast.GQLType
import com.apollographql.apollo.ast.GQLUnionTypeDefinition
import com.apollographql.apollo.ast.GQLValue
import com.apollographql.apollo.ast.Schema
import com.apollographql.apollo.ast.SourceLocation
import com.apollographql.apollo.ast.parseAsGQLValue
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import okio.Buffer
import okio.BufferedSource
import okio.ByteString.Companion.decodeHex
import kotlin.jvm.JvmMultifileClass
import kotlin.jvm.JvmName

/**
 * Because there are different versions of GraphQL in the wild and because users may use
 * different introspection queries it tries to be as lenient as possible.
 *
 * Probable user error (incomplete introspection query):
 * - missing description defaults to null
 * - missing directive argument definitions default to empty
 * - missing directives default to empty
 * - missing defaultValue defaults to null
 * - missing isDeprecated (for fields) defaults to false
 * - missing mutationType/subscriptionType
 *
 * Old schemas
 * - missing isRepeatable defaults to false
 * - missing isDeprecated (for input value) defaults to false
 *
 * Very old schemas
 * - missing directive locations skip the directive definition
 *
 */

interface IntrospectionSchema

@ApolloExperimental
fun BufferedSource.toIntrospectionSchema(filePath: String? = null): IntrospectionSchema {
  val bom = "EFBBBF".decodeHex()

  if (rangeEquals(0, bom)) {
    skip(bom.size.toLong())
  }

  val string = this.readUtf8()
  val envelope: REnvelope = json.decodeFromString(string)
  val __schema = envelope.__schema ?: envelope.data?.__schema
  if (__schema == null) {
    throw ConversionException("Invalid introspection schema '${string.substring(0, 50)}': expected input should look like '{ \"__schema\": { \"types\": ...'")
  }
  return IntrospectionSchemaImpl(__schema, filePath)
}

fun String.toIntrospectionSchema(): IntrospectionSchema = Buffer().writeUtf8(this).toIntrospectionSchema()

/**
 * Parses the [RSchema] into a [GQLDocument]
 */
fun IntrospectionSchema.toGQLDocument(): GQLDocument {
  this as IntrospectionSchemaImpl
  return GQLDocumentBuilder(this).toGQLDocument(filePath)
}

private class IntrospectionSchemaImpl(val __schema: RSchema, val filePath: String?) : IntrospectionSchema

private val json = Json {
  // be robust to errors: [] keys
  ignoreUnknownKeys = true
}

@Serializable
private class REnvelope(
    val data: RData? = null,
    @Suppress("PropertyName")
    val __schema: RSchema? = null,
)

@Serializable
private class RData(
    @Suppress("PropertyName")
    val __schema: RSchema,
)

@Serializable
private class RSchema(
    val description: Optional<String?> = Optional.absent(),
    val types: List<RTypeFull>,
    val queryType: RTypeRoot,
    val mutationType: Optional<RTypeRoot?> = Optional.absent(),
    val subscriptionType: Optional<RTypeRoot?> = Optional.absent(),
    val directives: Optional<List<RDirective>> = Optional.absent(),
)

@Serializable
private class RTypeRoot(val name: String)

@Serializable
private class RTypeFull(
    val kind: RTypeKind,
    val name: String,
    val description: Optional<String?> = Optional.absent(),
    val fields: List<RField>? = null,
    val interfaces: List<RTypeRef>? = null,
    val possibleTypes: List<RTypeRef>? = null,
    val enumValues: List<REnumValue>? = null,
    val inputFields: List<RInputValue>? = null,
    val specifiedByURL: String? = null,
    val isOneOf: Boolean? = null,
) {
  override fun toString(): String {
    return "$kind - $name"
  }
}

@Serializable
private class RTypeRef(
    val kind: RTypeKind,
    val name: String? = null,
    val ofType: RTypeRef? = null,
)

private enum class RTypeKind {
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
private class RField(
    val name: String,
    val description: Optional<String?> = Optional.absent(),
    val args: Optional<List<RInputValue>> = Optional.absent(),
    val type: RTypeRef,
    val isDeprecated: Optional<Boolean> = Optional.absent(),
    val deprecationReason: Optional<String?> = Optional.absent(),
)

@Serializable
private class RInputValue(
    val name: String,
    val description: Optional<String?> = Optional.absent(),
    val type: RTypeRef,
    val defaultValue: Optional<String?> = Optional.absent(),
    val isDeprecated: Boolean = false,
    val deprecationReason: String? = null,
)

@Serializable
private class REnumValue(
    val name: String,
    val description: Optional<String?> = Optional.absent(),
    val isDeprecated: Optional<Boolean> = Optional.absent(),
    val deprecationReason: Optional<String?> = Optional.absent(),
)

@Serializable
private class RDirective(
    val name: String,
    val description: Optional<String?> = Optional.absent(),
    val locations: Optional<List<String>> = Optional.absent(),
    val args: Optional<List<RInputValue>> = Optional.absent(),
    val isRepeatable: Boolean = false,
)

@Serializable(with = OptionalSerializer::class)
private sealed class Optional<out V> {
  fun getOrNull() = (this as? Present)?.value
  fun getOrThrow(): V {
    if (this !is Present) {
      throw Exception("Optional has no value")
    }
    return this.value
  }

  fun <R> mapValue(block: (V) -> R): Optional<R> {
    return when (this) {
      is Absent -> Absent
      is Present -> Optional.present(block(this.value))
    }
  }

  data class Present<V>(val value: V) : Optional<V>()
  object Absent : Optional<Nothing>()

  companion object {
    fun <V> absent(): Optional<V> = Absent

    fun <V> present(value: V): Optional<V> = Present(value)
  }
}


private class OptionalSerializer<T>(private val dataSerializer: KSerializer<T>) : KSerializer<Optional<T>> {
  override val descriptor: SerialDescriptor = dataSerializer.descriptor
  override fun serialize(encoder: Encoder, value: Optional<T>) {
    dataSerializer.serialize(encoder, value.getOrThrow())
  }

  override fun deserialize(decoder: Decoder): Optional<T> {
    return Optional.present(dataSerializer.deserialize(decoder))
  }
}

private class GQLDocumentBuilder(private val introspectionSchema: IntrospectionSchemaImpl) {
  fun toGQLDocument(filePath: String?): GQLDocument {
    return with(introspectionSchema.__schema) {
      val directives = if (directives is Optional.Absent) {
        println("Apollo: __schema.directives is missing, double check your introspection query")
        emptyList()
      } else {
        directives.getOrThrow()
      }

      GQLDocument(
          definitions = types.map {
            when (it.kind) {
              RTypeKind.SCALAR -> it.toGQLScalarTypeDefinition()
              RTypeKind.OBJECT -> it.toGQLObjectTypeDefinition()
              RTypeKind.INTERFACE -> it.toGQLInterfaceTypeDefinition()
              RTypeKind.UNION -> it.toGQLUnionTypeDefinition()
              RTypeKind.ENUM -> it.toGQLEnumTypeDefinition()
              RTypeKind.INPUT_OBJECT -> it.toGQLInputObjectTypeDefinition()
              else -> throw ConversionException("Unknown type kind: ${it.kind}")
            }
          }
              + directives.mapNotNull { it.toGQLDirectiveDefinition() }
              + schemaDefinition(),
          sourceLocation = SourceLocation.forPath(filePath)
      )
    }
  }

  private fun Optional<String?>.unwrapDescription(context: String): String? {
    return if (this is Optional.Absent) {
      println("Apollo: $context is missing 'description', double check your introspection query")
      null
    } else {
      getOrThrow()
    }
  }

  private fun RTypeFull.toGQLObjectTypeDefinition(): GQLObjectTypeDefinition {
    return GQLObjectTypeDefinition(
        description = description.unwrapDescription(name),
        name = name,
        directives = emptyList(),
        fields = fields?.map { it.toGQLFieldDefinition() } ?: throw ConversionException("Object '$name' does not define any field"),
        implementsInterfaces = findInterfacesImplementedBy(name)
    )
  }

  private fun findInterfacesImplementedBy(name: String): List<String> {
    return introspectionSchema.__schema
        .types
        .filter {
          it.kind == RTypeKind.INTERFACE
        }
        .filter {
          it.possibleTypes?.map { it.name }?.contains(name) == true
        }
        .map {
          it.name
        }
  }

  private fun RTypeFull.toGQLEnumTypeDefinition(): GQLEnumTypeDefinition {
    return GQLEnumTypeDefinition(
        description = description.unwrapDescription(name),
        name = name,
        enumValues = enumValues?.map { it.toGQLEnumValueDefinition() }
            ?: throw ConversionException("Enum '$name' does not define any value"),
        directives = emptyList()
    )
  }

  private fun REnumValue.toGQLEnumValueDefinition(): GQLEnumValueDefinition {
    return GQLEnumValueDefinition(
        description = description.unwrapDescription(name),
        name = name,
        directives = makeDirectives(deprecationReason.unwrapDeprecationReason(name))
    )
  }

  private fun Optional<String?>.unwrapDeprecationReason(name: String): String? {
    return if (this is Optional.Absent) {
      println("Apollo: $name is missing 'deprecationReason', double check your introspection query")
      null
    } else {
      getOrThrow()
    }
  }

  private fun RTypeFull.toGQLInterfaceTypeDefinition(): GQLInterfaceTypeDefinition {
    return GQLInterfaceTypeDefinition(
        name = name,
        description = description.unwrapDescription(name),
        fields = fields?.map { it.toGQLFieldDefinition() } ?: throw ConversionException("interface '$name' did not define any field"),
        implementsInterfaces = interfaces?.mapNotNull { it.name } ?: emptyList(),
        directives = emptyList()
    )
  }

  private fun RField.toGQLFieldDefinition(): GQLFieldDefinition {
    val args = if (args is Optional.Absent) {
        println("Apollo: $name.args is missing, double check your introspection query")
        emptyList()
    } else {
      args.getOrThrow()
    }

    return GQLFieldDefinition(
        name = name,
        description = description.unwrapDescription(name),
        arguments = args.map { it.toGQLInputValueDefinition() },
        directives = makeDirectives(deprecationReason.unwrapDeprecationReason(name)),
        type = type.toGQLType()
    )
  }

  private fun String?.toGQLValue(): GQLValue? {
    if (this == null) {
      // no default value
      return null
    }
    try {
      return Buffer().writeUtf8(this).parseAsGQLValue().getOrThrow()
    } catch (e: Exception) {
      throw ConversionException("cannot convert $this to a GQLValue")
    }
  }

  /**
   * This assumes the case where `deprecationReason == null && isDeprecated` is not valid
   * which is unclear from reading the spec as the below seems allowed:
   *
   * ```
   * type Query {
   *   foo: Int @deprecated(reason: null)
   * }
   * ```
   *
   * If there are legit use cases for `@deprecated(reason: null)` we should update this function
   */
  fun makeDirectives(deprecationReason: String?): List<GQLDirective> {
    if (deprecationReason == null) {
      return emptyList()
    }
    return listOf(
        GQLDirective(
            name = "deprecated",
            arguments = listOf(
                GQLArgument(name = "reason", value = GQLStringValue(value = deprecationReason))
            )
        )
    )
  }

  private fun RTypeFull.toGQLUnionTypeDefinition(): GQLUnionTypeDefinition {
    return GQLUnionTypeDefinition(
        name = name,
        description = description.unwrapDescription(name),
        memberTypes = possibleTypes?.map { it.toGQLNamedType() } ?: throw ConversionException("Union '$name' must have members"),
        directives = emptyList(),
    )
  }

  private fun RTypeRef.toGQLNamedType(): GQLNamedType {
    return toGQLType() as? GQLNamedType ?: throw ConversionException("expected a NamedType")
  }

  private fun RTypeRef.toGQLType(): GQLType {
    return when (this.kind) {
      RTypeKind.NON_NULL -> GQLNonNullType(
          type = ofType?.toGQLType() ?: throw ConversionException("ofType must not be null for non null types")
      )

      RTypeKind.LIST -> GQLListType(
          type = ofType?.toGQLType() ?: throw ConversionException("ofType must not be null for list types")
      )

      else -> GQLNamedType(
          name = name!!
      )
    }
  }

  private fun RSchema.schemaDefinition(): GQLSchemaDefinition {
    val rootOperationTypeDefinitions = mutableListOf<GQLOperationTypeDefinition>()
    rootOperationTypeDefinitions.add(
        GQLOperationTypeDefinition(
            operationType = "query",
            namedType = queryType.name
        )
    )
    val mutationType = if (mutationType is Optional.Absent) {
      println("Apollo: schema.mutationType is missing, double check your introspection query")
      null
    } else {
      mutationType.getOrThrow()
    }
    if (mutationType != null) {
      rootOperationTypeDefinitions.add(
          GQLOperationTypeDefinition(
              operationType = "mutation",
              namedType = mutationType.name
          )
      )
    }

    val subscriptionType = if (subscriptionType is Optional.Absent) {
      println("Apollo: schema.mutationType is missing, double check your introspection query")
      null
    } else {
      subscriptionType.getOrThrow()
    }
    if (subscriptionType != null) {
      rootOperationTypeDefinitions.add(
          GQLOperationTypeDefinition(
              operationType = "subscription",
              namedType = subscriptionType.name
          )
      )
    }

    return GQLSchemaDefinition(
        // Older versions of GraphQL do not have a description, do not warn on this
        description = description.getOrNull(),
        directives = emptyList(),
        rootOperationTypeDefinitions = rootOperationTypeDefinitions
    )
  }

  private fun RTypeFull.toGQLInputObjectTypeDefinition(): GQLInputObjectTypeDefinition {
    return GQLInputObjectTypeDefinition(
        description = description.unwrapDescription(name),
        name = name,
        inputFields = inputFields?.map { it.toGQLInputValueDefinition() }
            ?: throw ConversionException("Input Object '$name' does not define any input field"),
        directives = if (isOneOf == true) {
          listOf(
              GQLDirective(
                  name = Schema.ONE_OF,
                  arguments = emptyList()
              )
          )
        } else {
          emptyList()
        }
    )
  }

  private fun RInputValue.toGQLInputValueDefinition(): GQLInputValueDefinition {
    val defaultValue = if (defaultValue is Optional.Absent) {
      println("Apollo: '$name.defaultValue' is missing, check your introspection query")
      null
    } else {
      defaultValue.getOrNull()
    }
    return GQLInputValueDefinition(
        name = name,
        description = description.unwrapDescription(name),
        directives = makeDirectives(deprecationReason),
        defaultValue = defaultValue.toGQLValue(),
        type = type.toGQLType(),
    )
  }

  private fun RTypeFull.toGQLScalarTypeDefinition(): GQLScalarTypeDefinition {
    return GQLScalarTypeDefinition(
        description = description.unwrapDescription(name),
        name = name,
        directives = emptyList()
    )
  }

  private fun RDirective.toGQLDirectiveDefinition(): GQLDirectiveDefinition? {
    if (locations is Optional.Absent) {
      println("Apollo: '$name.locations' is missing, check your introspection query")
      return null
    }
    val locations = locations.getOrThrow()
    val args = if (args is Optional.Absent) {
      println("Apollo: '$name.args' is missing, check your introspection query")
      emptyList()
    } else {
      args.getOrThrow()
    }
    return GQLDirectiveDefinition(
        description = description.unwrapDescription(name),
        name = name,
        arguments = args.map { it.toGQLInputValueDefinition() },
        locations = locations.map { GQLDirectiveLocation.valueOf(it) },
        repeatable = isRepeatable,
    )
  }
}

@ApolloInternal
fun IntrospectionSchema.normalize(): IntrospectionSchema {
  this as IntrospectionSchemaImpl
  // This does not sort the fields/arguments for some reason
  return with(__schema) {
    IntrospectionSchemaImpl(
        RSchema(
            queryType = queryType,
            mutationType = mutationType,
            subscriptionType = subscriptionType,
            types = types.sortedBy { it.name },
            directives = directives.mapValue { it.sortedBy { it.name } },
            description = description
        ),
        null
    )
  }
}




