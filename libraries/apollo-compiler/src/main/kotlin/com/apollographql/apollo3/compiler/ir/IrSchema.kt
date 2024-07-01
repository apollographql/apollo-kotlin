package com.apollographql.apollo.compiler.ir

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
import com.apollographql.apollo.ast.GQLScalarTypeDefinition
import com.apollographql.apollo.ast.GQLType
import com.apollographql.apollo.ast.GQLUnionTypeDefinition
import com.apollographql.apollo.ast.Schema
import com.apollographql.apollo.ast.Schema.Companion.TYPE_POLICY
import com.apollographql.apollo.ast.fieldDefinitions
import com.apollographql.apollo.ast.findDeprecationReason
import com.apollographql.apollo.ast.findOneOf
import com.apollographql.apollo.ast.findOptInFeature
import com.apollographql.apollo.ast.findTargetName
import com.apollographql.apollo.ast.internal.toConnectionFields
import com.apollographql.apollo.ast.internal.toEmbeddedFields
import com.apollographql.apollo.compiler.UsedCoordinates
import com.apollographql.apollo.compiler.codegen.keyArgs
import com.apollographql.apollo.compiler.codegen.paginationArgs
import kotlinx.serialization.Serializable

@Serializable
internal class DefaultIrSchema(
    val irScalars: List<IrScalar>,
    val irEnums: List<IrEnum>,
    val irInputObjects: List<IrInputObject>,
    val irUnions: List<IrUnion>,
    val irInterfaces: List<IrInterface>,
    val irObjects: List<IrObject>,
    val connectionTypes: List<String>,
) : IrSchema

interface IrSchema

internal sealed interface IrSchemaType {
  val name: String
}

@Serializable
internal data class IrInputObject(
    override val name: String,
    val description: String?,
    val deprecationReason: String?,
    val fields: List<IrInputField>,
    val isOneOf: Boolean,
) : IrSchemaType

@Serializable
internal data class IrObject(
    override val name: String,
    val implements: List<String>,
    /**
     * contrary to [implements], [superTypes] also includes unions
     */
    val superTypes: List<String>,
    val keyFields: List<String>,
    val description: String?,
    val deprecationReason: String?,
    val embeddedFields: List<String>,
    val mapProperties: List<IrMapProperty>,
    val fieldDefinitions: List<IrFieldDefinition>,
) : IrSchemaType


@Serializable
internal data class IrInterface(
    override val name: String,
    val implements: List<String>,
    val keyFields: List<String>,
    val description: String?,
    val deprecationReason: String?,
    val embeddedFields: List<String>,
    val mapProperties: List<IrMapProperty>,
    val fieldDefinitions: List<IrFieldDefinition>,
) : IrSchemaType

@Serializable
internal data class IrUnion(
    override val name: String,
    val members: List<String>,
    val description: String?,
    val deprecationReason: String?,
) : IrSchemaType

@Serializable
internal data class IrScalar(
    override val name: String,
    val description: String?,
    val deprecationReason: String?,
) : IrSchemaType {
  val type = IrScalarType(name, nullable = true)
}

/**
 * This is a separate type from [IrType] because [IrType] is quite big already. We might
 * want to refactor our type handling at some point
 */
@Serializable
internal sealed interface IrType2

@Serializable
internal class IrNonNullType2(val ofType: IrType2) : IrType2

@Serializable
internal class IrListType2(val ofType: IrType2) : IrType2

@Serializable
internal class IrScalarType2(val name: String) : IrType2

@Serializable
internal class IrEnumType2(val name: String) : IrType2

@Serializable
internal class IrCompositeType2(val name: String) : IrType2

@Serializable
internal data class IrArgumentDefinition(
    val id: String,
    val name: String,
    val propertyName: String,
    val isKey: Boolean,
    val isPagination: Boolean,
) {
  companion object {
    fun id(type: String, field: String, argument: String) = "$type.$field.$argument"
    fun propertyName(fieldName: String, argumentName: String) = "__${fieldName}_${argumentName}"
  }
}

@Serializable
internal data class IrMapProperty(
    val name: String,
    val type: IrType2,
)

@Serializable
internal data class IrFieldDefinition(
    val name: String,
    val type: IrType2,
    val argumentDefinitions: List<IrArgumentDefinition>,
)

@Serializable
internal data class IrEnum(
    override val name: String,
    val description: String?,
    val values: List<Value>,
) : IrSchemaType {
  val type = IrEnumType(name, nullable = true)

  @Serializable
  data class Value(
      val name: String,
      val targetName: String,
      val description: String?,
      val deprecationReason: String?,
      val optInFeature: String?,
  )
}

/**
 * An input field
 *
 * Note: [IrInputField], and [IrVariable] are all very similar since they all share
 * the [com.apollographql.apollo.ast.GQLInputValueDefinition] type, but [IrVariable]
 * misses description, deprecation and optIn so they are modeled differently in
 * [IrOperations]
 */
@Serializable
internal data class IrInputField(
    val name: String,
    val description: String?,
    val deprecationReason: String?,
    val optInFeature: String?,
    val type: IrType,
)

internal fun GQLEnumTypeDefinition.toIr(schema: Schema): IrEnum {
  return IrEnum(
      name = name,
      description = description,
      values = enumValues.map { it.toIr(schema) }
  )
}

internal fun GQLEnumValueDefinition.toIr(schema: Schema): IrEnum.Value {
  return IrEnum.Value(
      name = name,
      targetName = directives.findTargetName(schema) ?: name,
      description = description,
      deprecationReason = directives.findDeprecationReason(),
      optInFeature = directives.findOptInFeature(schema),
  )
}

internal fun GQLUnionTypeDefinition.toIr(): IrUnion {
  return IrUnion(
      name = name,
      members = memberTypes.map { it.name },
      description = description,
      // XXX: this is not spec-compliant. Directive cannot be on union definitions
      deprecationReason = directives.findDeprecationReason()
  )
}

internal fun GQLScalarTypeDefinition.toIr(): IrScalar {
  return IrScalar(
      name = name,
      description = description,
      // XXX: this is not spec-compliant. Directive cannot be on scalar definitions
      deprecationReason = directives.findDeprecationReason()
  )
}

internal fun GQLInputObjectTypeDefinition.toIr(schema: Schema): IrInputObject {
  return IrInputObject(
      name = name,
      description = description,
      // XXX: this is not spec-compliant. Directive cannot be on input objects definitions
      deprecationReason = directives.findDeprecationReason(),
      fields = inputFields.map { it.toIrInputField(schema) },
      isOneOf = directives.findOneOf(),
  )
}

internal fun GQLInterfaceTypeDefinition.toIr(schema: Schema, usedCoordinates: UsedCoordinates): IrInterface {
  return IrInterface(
      name = name,
      implements = implementsInterfaces,
      keyFields = schema.keyFields(name).toList(),
      description = description,
      // XXX: this is not spec-compliant. Deprecation directives cannot be on interfaces
      // See https://spec.graphql.org/draft/#sec--deprecated
      deprecationReason = directives.findDeprecationReason(),
      embeddedFields = directives.filter { schema.originalDirectiveName(it.name) == TYPE_POLICY }.toEmbeddedFields() +
          directives.filter { schema.originalDirectiveName(it.name) == TYPE_POLICY }.toConnectionFields() +
          connectionTypeEmbeddedFields(name, schema),
      mapProperties = this.fields.filter {
        usedCoordinates.hasField(type = name, field = it.name)
      }.map {
        it.toIrMapProperty(schema)
      },
      fieldDefinitions = this.fieldDefinitions(schema).filter {
        usedCoordinates.hasField(type = name, field = it.name)
      }.mapNotNull {
        it.toIrFieldDefinition(schema, name, usedCoordinates)
            // Only include fields that have arguments used in operations
            .takeIf { it.argumentDefinitions.isNotEmpty() }
      },
  )
}


/**
 * If [typeName] is declared as a Relay Connection type (via the `@typePolicy` directive), return the standard arguments
 * to be embedded.
 * Otherwise, return an empty set.
 */
private fun connectionTypeEmbeddedFields(typeName: String, schema: Schema): Set<String> {
  return if (typeName in schema.connectionTypes) {
    setOf("edges", "pageInfo")
  } else {
    emptySet()
  }
}

internal fun GQLObjectTypeDefinition.toIr(schema: Schema, usedCoordinates: UsedCoordinates): IrObject {
  return IrObject(
      name = name,
      implements = implementsInterfaces,
      keyFields = schema.keyFields(name).toList(),
      description = description,
      // XXX: this is not spec-compliant. Directive cannot be on object definitions
      deprecationReason = directives.findDeprecationReason(),
      embeddedFields = directives.filter { schema.originalDirectiveName(it.name) == TYPE_POLICY }.toEmbeddedFields() +
          directives.filter { schema.originalDirectiveName(it.name) == TYPE_POLICY }.toConnectionFields() +
          connectionTypeEmbeddedFields(name, schema),
      mapProperties = this.fields.filter {
        usedCoordinates.hasField(type = name, field = it.name)
      }.map {
        it.toIrMapProperty(schema)
      },
      fieldDefinitions = this.fieldDefinitions(schema).filter {
        usedCoordinates.hasField(type = name, field = it.name)
      }.mapNotNull {
        it.toIrFieldDefinition(schema, name, usedCoordinates)
            // Only include fields that have arguments used in operations
            .takeIf { it.argumentDefinitions.isNotEmpty() }
      },
      superTypes = schema.superTypes(this).toList()
  )
}

private fun GQLFieldDefinition.toIrMapProperty(schema: Schema): IrMapProperty {
  return IrMapProperty(
      name,
      type.toIrType2(schema)
  )
}

private fun GQLFieldDefinition.toIrFieldDefinition(
    schema: Schema,
    parentType: String,
    usedCoordinates: UsedCoordinates,
): IrFieldDefinition {
  val typeDefinition = schema.typeDefinition(parentType)
  val keyArgs = typeDefinition.keyArgs(name, schema)
  val paginationArgs = typeDefinition.paginationArgs(name, schema)
  return IrFieldDefinition(
      name = name,
      type = type.toIrType2(schema),
      argumentDefinitions = arguments.mapNotNull {
        // We only include arguments that are used in operations
        if (!usedCoordinates.hasArgument(type = parentType, field = name, argument = it.name)) {
          null
        } else {
          IrArgumentDefinition(
              id = IrArgumentDefinition.id(parentType, name, it.name),
              propertyName = IrArgumentDefinition.propertyName(name, it.name),
              name = it.name,
              isKey = keyArgs.contains(it.name),
              isPagination = paginationArgs.contains(it.name)
          )
        }
      }
  )
}

private fun GQLType.toIrType2(schema: Schema): IrType2 {
  return when (this) {
    is GQLNonNullType -> IrNonNullType2(type.toIrType2(schema))
    is GQLListType -> IrListType2(type.toIrType2(schema))
    is GQLNamedType -> {
      when (schema.typeDefinition(name)) {
        is GQLScalarTypeDefinition -> return IrScalarType2(name)
        is GQLEnumTypeDefinition -> return IrEnumType2(name)
        is GQLInputObjectTypeDefinition -> error("Input objects are not supported in data builders")
        is GQLInterfaceTypeDefinition,
        is GQLObjectTypeDefinition,
        is GQLUnionTypeDefinition,
        -> IrCompositeType2(name)
      }
    }
  }
}


/**
 * This is not named `toIr` as [GQLInputValueDefinition] also maps to variables and arguments
 */
private fun GQLInputValueDefinition.toIrInputField(schema: Schema): IrInputField {
  var irType = type.toIr(schema)
  if (type !is GQLNonNullType || defaultValue != null) {
    /**
     * Contrary to [IrVariable], we default to making input fields optional as they are out of control of the user, and
     * we don't want to force users to fill all values to define an input object
     */
    irType = irType.optional(true)
  }
  return IrInputField(
      name = name,
      description = description,
      deprecationReason = directives.findDeprecationReason(),
      optInFeature = directives.findOptInFeature(schema),
      type = irType,
  )
}


internal fun IrType2.isCompositeOrWrappedComposite(): Boolean {
  return when (this) {
    is IrScalarType2 -> false
    is IrEnumType2 -> false
    is IrNonNullType2 -> ofType.isCompositeOrWrappedComposite()
    is IrListType2 -> ofType.isCompositeOrWrappedComposite()
    else -> true
  }
}
