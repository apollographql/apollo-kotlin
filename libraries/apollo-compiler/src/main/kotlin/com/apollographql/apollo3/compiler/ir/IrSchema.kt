package com.apollographql.apollo3.compiler.ir

import com.apollographql.apollo3.ast.GQLEnumTypeDefinition
import com.apollographql.apollo3.ast.GQLEnumValueDefinition
import com.apollographql.apollo3.ast.GQLFieldDefinition
import com.apollographql.apollo3.ast.GQLInputObjectTypeDefinition
import com.apollographql.apollo3.ast.GQLInputValueDefinition
import com.apollographql.apollo3.ast.GQLInterfaceTypeDefinition
import com.apollographql.apollo3.ast.GQLListType
import com.apollographql.apollo3.ast.GQLNamedType
import com.apollographql.apollo3.ast.GQLNonNullType
import com.apollographql.apollo3.ast.GQLObjectTypeDefinition
import com.apollographql.apollo3.ast.GQLScalarTypeDefinition
import com.apollographql.apollo3.ast.GQLType
import com.apollographql.apollo3.ast.GQLUnionTypeDefinition
import com.apollographql.apollo3.ast.Schema
import com.apollographql.apollo3.ast.Schema.Companion.TYPE_POLICY
import com.apollographql.apollo3.ast.coerceInExecutableContextOrThrow
import com.apollographql.apollo3.ast.findDeprecationReason
import com.apollographql.apollo3.ast.findOptInFeature
import com.apollographql.apollo3.ast.findTargetName
import com.apollographql.apollo3.ast.internal.toConnectionFields
import com.apollographql.apollo3.ast.internal.toEmbeddedFields


internal class DefaultIrSchema(
    val irScalars: List<IrScalar>,
    val irEnums: List<IrEnum>,
    val irInputObjects: List<IrInputObject>,
    val irUnions: List<IrUnion>,
    val irInterfaces: List<IrInterface>,
    val irObjects: List<IrObject>,

    val connectionTypes: List<String>,
) : IrSchema {
  val allTypes: List<IrSchemaType>
    get() {
      return (irScalars + irEnums + irInputObjects + irUnions + irInterfaces + irObjects).sortedBy { it.name }
    }
}

interface IrSchema

internal sealed interface IrSchemaType {
  val name: String
  val targetName: String?
}

internal data class IrInputObject(
    override val name: String,
    override val targetName: String?,
    val description: String?,
    val deprecationReason: String?,
    val fields: List<IrInputField>,
) : IrSchemaType

internal data class IrObject(
    override val name: String,
    override val targetName: String?,
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
) : IrSchemaType


internal data class IrInterface(
    override val name: String,
    override val targetName: String?,
    val implements: List<String>,
    val keyFields: List<String>,
    val description: String?,
    val deprecationReason: String?,
    val embeddedFields: List<String>,
    val mapProperties: List<IrMapProperty>,
) : IrSchemaType

internal data class IrUnion(
    override val name: String,
    override val targetName: String?,
    val members: List<String>,
    val description: String?,
    val deprecationReason: String?,
) : IrSchemaType

internal data class IrScalar(
    override val name: String,
    override val targetName: String?,
    val description: String?,
    val deprecationReason: String?,
) : IrSchemaType {
  val type = IrScalarType(name)
}

/**
 * This is a separate type from [IrType] because [IrType] is quite big already. We might
 * want to refactor our type handling at some point
 */
internal sealed interface IrType2
internal class IrNonNullType2(val ofType: IrType2) : IrType2
internal class IrListType2(val ofType: IrType2) : IrType2
internal class IrScalarType2(val name: String) : IrType2
internal class IrEnumType2(val name: String) : IrType2
internal class IrCompositeType2(val name: String) : IrType2

internal data class IrMapProperty(
    val name: String,
    val type: IrType2,
)

internal data class IrEnum(
    override val name: String,
    override val targetName: String?,
    val description: String?,
    val values: List<Value>,
) : IrSchemaType {
  val type = IrEnumType(name)

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
 * the [com.apollographql.apollo3.ast.GQLInputValueDefinition] type, but [IrVariable]
 * so they are modeled differently in the [IrOperations]
 */
internal data class IrInputField(
    val name: String,
    val description: String?,
    val deprecationReason: String?,
    val optInFeature: String?,
    val type: IrType,
    val defaultValue: IrValue?,
)

internal fun GQLEnumTypeDefinition.toIr(schema: Schema): IrEnum {
  return IrEnum(
      name = name,
      targetName = directives.findTargetName(schema),
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

internal fun GQLUnionTypeDefinition.toIr(schema: Schema): IrUnion {
  return IrUnion(
      name = name,
      targetName = directives.findTargetName(schema),
      members = memberTypes.map { it.name },
      description = description,
      // XXX: this is not spec-compliant. Directive cannot be on union definitions
      deprecationReason = directives.findDeprecationReason()
  )
}

internal fun GQLScalarTypeDefinition.toIr(schema: Schema): IrScalar {
  return IrScalar(
      name = name,
      targetName = directives.findTargetName(schema),
      description = description,
      // XXX: this is not spec-compliant. Directive cannot be on scalar definitions
      deprecationReason = directives.findDeprecationReason()
  )
}

internal fun GQLInputObjectTypeDefinition.toIr(schema: Schema): IrInputObject {
  return IrInputObject(
      name = name,
      targetName = directives.findTargetName(schema),
      description = description,
      // XXX: this is not spec-compliant. Directive cannot be on input objects definitions
      deprecationReason = directives.findDeprecationReason(),
      fields = inputFields.map { it.toIrInputField(schema) }
  )
}

internal fun GQLInterfaceTypeDefinition.toIr(schema: Schema, usedFields: Map<String, Set<String>>): IrInterface {
  /**
   * If generateDataBuilders is false, we do not track usedFields, fallback to an emptySet
   */
  val fields = usedFields.get(name) ?: emptySet()

  return IrInterface(
      name = name,
      targetName = directives.findTargetName(schema),
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
        fields.contains(it.name)
      }.map {
        it.toIrMapProperty(schema)
      }
  )
}


/**
 * If [typeName] is declared as a Relay Connection type (via the `@typePolicy` directive), return the standard arguments
 * to be embedded.
 * Otherwise, return an empty set.
 */
private fun connectionTypeEmbeddedFields(typeName: String, schema: Schema): Set<String> {
  return if (typeName in schema.connectionTypes) {
    setOf("edges")
  } else {
    emptySet()
  }
}

internal fun GQLObjectTypeDefinition.toIr(schema: Schema, usedFields: Map<String, Set<String>>): IrObject {
  val fields = usedFields.get(name) ?: emptySet()

  return IrObject(
      name = name,
      targetName = directives.findTargetName(schema),
      implements = implementsInterfaces,
      keyFields = schema.keyFields(name).toList(),
      description = description,
      // XXX: this is not spec-compliant. Directive cannot be on object definitions
      deprecationReason = directives.findDeprecationReason(),
      embeddedFields = directives.filter { schema.originalDirectiveName(it.name) == TYPE_POLICY }.toEmbeddedFields() +
          directives.filter { schema.originalDirectiveName(it.name) == TYPE_POLICY }.toConnectionFields() +
          connectionTypeEmbeddedFields(name, schema),
      mapProperties = this.fields.filter {
        fields.contains(it.name)
      }.map {
        it.toIrMapProperty(schema)
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
  val coercedDefaultValue = defaultValue?.coerceInExecutableContextOrThrow(type, schema)

  var irType = type.toIr(schema)
  if (type !is GQLNonNullType || coercedDefaultValue != null) {
    /**
     * Contrary to [IrVariable], we default to making input fields optional as they are out of control of the user, and
     * we don't want to force users to fill all values to define an input object
     */
    irType = irType.makeOptional()
  }
  return IrInputField(
      name = name,
      description = description,
      deprecationReason = directives.findDeprecationReason(),
      optInFeature = directives.findOptInFeature(schema),
      type = irType,
      defaultValue = coercedDefaultValue?.toIrValue(),
  )
}
