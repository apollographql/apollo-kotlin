package com.apollographql.apollo3.compiler.introspection

import com.apollographql.apollo3.annotations.ApolloExperimental
import com.apollographql.apollo3.ast.ConversionException
import com.apollographql.apollo3.ast.GQLArgument
import com.apollographql.apollo3.ast.GQLArguments
import com.apollographql.apollo3.ast.GQLBooleanValue
import com.apollographql.apollo3.ast.GQLDirective
import com.apollographql.apollo3.ast.GQLDocument
import com.apollographql.apollo3.ast.GQLEnumTypeDefinition
import com.apollographql.apollo3.ast.GQLEnumValueDefinition
import com.apollographql.apollo3.ast.GQLFieldDefinition
import com.apollographql.apollo3.ast.GQLFloatValue
import com.apollographql.apollo3.ast.GQLInputObjectTypeDefinition
import com.apollographql.apollo3.ast.GQLInputValueDefinition
import com.apollographql.apollo3.ast.GQLIntValue
import com.apollographql.apollo3.ast.GQLInterfaceTypeDefinition
import com.apollographql.apollo3.ast.GQLListType
import com.apollographql.apollo3.ast.GQLListValue
import com.apollographql.apollo3.ast.GQLNamedType
import com.apollographql.apollo3.ast.GQLNonNullType
import com.apollographql.apollo3.ast.GQLObjectField
import com.apollographql.apollo3.ast.GQLObjectTypeDefinition
import com.apollographql.apollo3.ast.GQLObjectValue
import com.apollographql.apollo3.ast.GQLOperationTypeDefinition
import com.apollographql.apollo3.ast.GQLScalarTypeDefinition
import com.apollographql.apollo3.ast.GQLSchemaDefinition
import com.apollographql.apollo3.ast.GQLStringValue
import com.apollographql.apollo3.ast.GQLType
import com.apollographql.apollo3.ast.GQLUnionTypeDefinition
import com.apollographql.apollo3.ast.GQLValue
import com.apollographql.apollo3.ast.Schema
import com.apollographql.apollo3.ast.SourceLocation
import com.apollographql.apollo3.ast.parseAsGQLDocument
import com.apollographql.apollo3.ast.parseAsGQLValue
import com.apollographql.apollo3.ast.toSchema
import com.apollographql.apollo3.ast.validateAsSchema
import com.apollographql.apollo3.ast.withoutBuiltinDefinitions
import com.apollographql.apollo3.compiler.buffer
import okio.buffer
import okio.source
import java.io.File

@OptIn(ApolloExperimental::class)
private class GQLDocumentBuilder(private val introspectionSchema: IntrospectionSchema, filePath: String?) {
  private val sourceLocation = SourceLocation(
      filePath = filePath,
      line = -1,
      position = -1
  )

  fun toGQLDocument(): GQLDocument {
    return with(introspectionSchema.__schema) {
      GQLDocument(
          definitions = types.map {
            when (it) {
              is IntrospectionSchema.Schema.Type.Union -> it.toGQLUnionTypeDefinition()
              is IntrospectionSchema.Schema.Type.Interface -> it.toGQLInterfaceTypeDefinition()
              is IntrospectionSchema.Schema.Type.Enum -> it.toGQLEnumTypeDefinition()
              is IntrospectionSchema.Schema.Type.Object -> it.toGQLObjectTypeDefinition()
              is IntrospectionSchema.Schema.Type.InputObject -> it.toGQLInputObjectTypeDefinition()
              is IntrospectionSchema.Schema.Type.Scalar -> it.toGQLScalarTypeDefinition()
            }
          } + schemaDefinition(),
          filePath = sourceLocation.filePath
      )
    }
  }

  private fun IntrospectionSchema.Schema.Type.Object.toGQLObjectTypeDefinition(): GQLObjectTypeDefinition {
    return GQLObjectTypeDefinition(
        sourceLocation = sourceLocation,
        description = description,
        name = name,
        directives = emptyList(),
        fields = fields?.map { it.toGQLFieldDefinition() } ?: throw ConversionException("Object '$name' did not define any field"),
        implementsInterfaces = findInterfacesImplementedBy(name)
    )
  }

  private fun findInterfacesImplementedBy(name: String): List<String> {
    return introspectionSchema.__schema.types.filterIsInstance<IntrospectionSchema.Schema.Type.Interface>()
        .filter {
          it.possibleTypes?.map { it.name }?.contains(name) == true
        }
        .map {
          it.name
        }
  }

  private fun IntrospectionSchema.Schema.Type.Enum.toGQLEnumTypeDefinition(): GQLEnumTypeDefinition {
    return GQLEnumTypeDefinition(
        sourceLocation = sourceLocation,
        description = description,
        name = name,
        enumValues = enumValues.map { it.toGQLEnumValueDefinition() },
        directives = emptyList()
    )
  }

  private fun IntrospectionSchema.Schema.Type.Enum.Value.toGQLEnumValueDefinition(): GQLEnumValueDefinition {
    return GQLEnumValueDefinition(
        sourceLocation = sourceLocation,
        description = description,
        name = name,
        directives = makeDirectives(deprecationReason)
    )
  }

  private fun IntrospectionSchema.Schema.Type.Interface.toGQLInterfaceTypeDefinition(): GQLInterfaceTypeDefinition {
    return GQLInterfaceTypeDefinition(
        sourceLocation = sourceLocation,
        name = name,
        description = description,
        fields = fields?.map { it.toGQLFieldDefinition() } ?: throw ConversionException("interface '$name' did not define any field"),
        implementsInterfaces = emptyList(), // TODO
        directives = emptyList()
    )
  }

  private fun IntrospectionSchema.Schema.Field.toGQLFieldDefinition(): GQLFieldDefinition {
    return GQLFieldDefinition(
        sourceLocation = sourceLocation,
        name = name,
        description = description,
        arguments = this.args.map { it.toGQLInputValueDefinition() },
        directives = makeDirectives(deprecationReason),
        type = type.toGQLType()
    )
  }

  private fun IntrospectionSchema.Schema.Field.Argument.toGQLInputValueDefinition(): GQLInputValueDefinition {
    return GQLInputValueDefinition(
        sourceLocation = sourceLocation,
        name = name,
        description = description,
        directives = makeDirectives(deprecationReason),
        defaultValue = defaultValue.toGQLValue(),
        type = type.toGQLType(),
    )
  }

  private fun Any?.toGQLValue(): GQLValue? {
    if (this == null) {
      // no default value
      return null
    }
    try {
      if (this is String) {
        return buffer().parseAsGQLValue().valueAssertNoErrors()
      }
    } catch (e: Exception) {
      println("Wrongly encoded default value: $this: ${e.message}")
    }

    // All the below should theoretically not happen because the spec says defaultValue should be
    // a GQLValue encoded as a string
    return when {
      this is String -> GQLStringValue(value = this)
      this is Int -> GQLIntValue(value = this)
      this is Long -> GQLIntValue(value = this.toInt())
      this is Double -> GQLFloatValue(value = this)
      this is Boolean -> GQLBooleanValue(value = this)
      this is Map<*, *> -> GQLObjectValue(fields = this.map {
        GQLObjectField(name = it.key as String, value = it.value.toGQLValue()!!)
      })
      this is List<*> -> GQLListValue(values = map { it.toGQLValue()!! })
      else -> throw ConversionException("cannot convert $this to a GQLValue")
    }
  }

  fun makeDirectives(deprecationReason: String?): List<GQLDirective> {
    if (deprecationReason == null) {
      return emptyList()
    }
    return listOf(
        GQLDirective(
            name = "deprecated",
            arguments = GQLArguments(listOf(
                GQLArgument(name = "reason", value = GQLStringValue(value = deprecationReason))
            ))
        )
    )
  }

  private fun IntrospectionSchema.Schema.Type.Union.toGQLUnionTypeDefinition(): GQLUnionTypeDefinition {
    return GQLUnionTypeDefinition(
        sourceLocation = sourceLocation,
        name = name,
        description = "",
        memberTypes = possibleTypes?.map { it.toGQLNamedType() } ?: throw ConversionException("Union '$name' must have members"),
        directives = emptyList(),
    )
  }

  private fun IntrospectionSchema.Schema.TypeRef.toGQLNamedType(): GQLNamedType {
    return toGQLType() as? GQLNamedType ?: throw ConversionException("expected a NamedType")
  }

  private fun IntrospectionSchema.Schema.TypeRef.toGQLType(): GQLType {
    return when (this.kind) {
      IntrospectionSchema.Schema.Kind.NON_NULL -> GQLNonNullType(
          type = ofType?.toGQLType() ?: throw ConversionException("ofType must not be null for non null types")
      )
      IntrospectionSchema.Schema.Kind.LIST -> GQLListType(
          type = ofType?.toGQLType() ?: throw ConversionException("ofType must not be null for list types")
      )
      else -> GQLNamedType(
          name = name!!
      )
    }
  }

  private fun IntrospectionSchema.Schema.schemaDefinition(): GQLSchemaDefinition {
    val rootOperationTypeDefinitions = mutableListOf<GQLOperationTypeDefinition>()
    rootOperationTypeDefinitions.add(
        GQLOperationTypeDefinition(
            sourceLocation = sourceLocation,
            operationType = "query",
            namedType = queryType.name
        )
    )
    if (mutationType != null) {
      rootOperationTypeDefinitions.add(
          GQLOperationTypeDefinition(
              sourceLocation = sourceLocation,
              operationType = "mutation",
              namedType = mutationType.name
          )
      )
    }
    if (subscriptionType != null) {
      rootOperationTypeDefinitions.add(
          GQLOperationTypeDefinition(
              sourceLocation = sourceLocation,
              operationType = "subscription",
              namedType = subscriptionType.name
          )
      )
    }

    return GQLSchemaDefinition(
        sourceLocation = sourceLocation,
        description = "",
        directives = emptyList(),
        rootOperationTypeDefinitions = rootOperationTypeDefinitions
    )
  }

  private fun IntrospectionSchema.Schema.Type.InputObject.toGQLInputObjectTypeDefinition(): GQLInputObjectTypeDefinition {
    return GQLInputObjectTypeDefinition(
        sourceLocation = sourceLocation,
        description = description,
        name = name,
        inputFields = inputFields.map { it.toGQLInputValueDefinition() },
        directives = emptyList()
    )
  }

  private fun IntrospectionSchema.Schema.InputField.toGQLInputValueDefinition(): GQLInputValueDefinition {
    return GQLInputValueDefinition(
        sourceLocation = sourceLocation,
        description = description,
        name = name,
        directives = makeDirectives(deprecationReason),
        type = type.toGQLType(),
        defaultValue = defaultValue.toGQLValue()
    )
  }

  private fun IntrospectionSchema.Schema.Type.Scalar.toGQLScalarTypeDefinition(): GQLScalarTypeDefinition {
    return GQLScalarTypeDefinition(
        sourceLocation = sourceLocation,
        description = description,
        name = name,
        directives = emptyList()
    )
  }
}

/**
 * Parses the [IntrospectionSchema] into a [GQLDocument]
 *
 * The returned [GQLDocument] does not contain any of the builtin definitions (scalars, directives, introspection)
 *
 * See https://spec.graphql.org/draft/#sel-GAHXJHABuCB_Dn6F
 */
@ApolloExperimental
fun IntrospectionSchema.toGQLDocument(filePath: String? = null): GQLDocument = GQLDocumentBuilder(this, filePath)
    .toGQLDocument()
    /**
     * Introspection already contains builtin types like Int, Boolean, __Schema, etc...
     */
    .withoutBuiltinDefinitions()

/**
 * Transforms the [IntrospectionSchema] into a [Schema] that contains builtin definitions
 *
 * In the process, the builtin definitions are removed and added again.
 */
@ApolloExperimental
fun IntrospectionSchema.toSchema(): Schema = toGQLDocument().validateAsSchema().valueAssertNoErrors()

/**
 * Transforms the given file to a [GQLDocument] without doing validation
 */
@ApolloExperimental
fun File.toSchemaGQLDocument(): GQLDocument {
  return if (extension == "json") {
    toIntrospectionSchema().toGQLDocument(filePath = path)
  } else {
    source().buffer().parseAsGQLDocument(filePath = path).valueAssertNoErrors()
  }
}

@ApolloExperimental
fun File.toSchema(): Schema = toSchemaGQLDocument().validateAsSchema().valueAssertNoErrors()