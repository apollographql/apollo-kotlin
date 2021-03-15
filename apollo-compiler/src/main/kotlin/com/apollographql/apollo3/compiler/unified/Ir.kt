package com.apollographql.apollo3.compiler.unified

/*
* Unified IR. This builds all the possible field trees. As polymorphic fields are used, this can become
* quite large and will be pruned in a later step
* - moves @include/@skip directives on inline fragments and object fields to their children selections
* - interprets @deprecated directives
* - coerce argument values and resolves defaultValue
* - infers fragment variables
* - more generally removes all references to the GraphQL AST and "embeds" type definitions/field definitions
*/
data class UnifiedIr(
    val operations: List<IrOperation>,
    val namedFragments: List<IrNamedFragment>,
    val inputObjects: List<IrInputObject>,
    val enums: List<IrEnum>,
    val customScalars: List<IrCustomScalar>
)

data class IrEnum(
    val name: String,
    val description: String?,
    val values: List<IrEnumValue>,

    )

data class IrEnumValue(
    val name: String,
    val description: String?,
    val deprecationReason: String?
)

/**
 * An input field
 *
 * Note: [IrInputField], [IrArgument] and [IrVariable] are all very similar since they all share
 * the [com.apollographql.apollo3.compiler.frontend.GQLInputValueDefinition] type but they have
 * also differences:
 * - [IrArgument] also has a value in addition to the type definition
 * - [IrVariable] doesn't have a description
 */
data class IrInputField(
    val name: String,
    val description: String?,
    val deprecationReason: String?,
    val type: IrType,
    val defaultValue: IrValue?
)

data class IrOperation(
    val name: String,
    val operationType: IrOperationType,
    val typeCondition: String,
    val variables: List<IrVariable>,
    val description: String?,
    val dataField: IrField,
    val sourceWithFragments: String,
)

data class IrNamedFragment(
    val name: String,
    val description: String?,
    val dataField: IrField,
    /**
     * Fragments do not have variables per-se but we can infer them from the document
     * Default values will always be null for those
     */
    val variables: List<IrVariable>,
    val typeCondition: String,
)

enum class IrOperationType {
  Query,
  Mutation,
  Subscription
}

data class IrField(
    val name: String,
    val alias: String?,
    val arguments: List<IrArgument>,
    // from the fieldDefinition
    val description: String?,
    // from the fieldDefinition
    val type: IrType,
    // from the fieldDefinition directives
    val deprecationReason: String?,

    val condition: BooleanExpression,
    // empty for a scalar field
    val fieldSets: List<IrFieldSet>,
) {
  val responseName = alias ?: name
}

/**
 * @param possibleTypes: the possibleTypes that will map to this [IrFieldSet]. If it is empty,
 */
data class IrFieldSet(
    val typeSet: Set<String>,
    val possibleTypes: Set<String>,
    val fields: List<IrField>,
    val implements: Set<TypeSet>,
)

data class IrInputObject(
    val name: String,
    val description: String?,
    val deprecationReason: String?,
    val fields: List<IrInputField>
)

data class IrCustomScalar(
    val name: String,
)

/**
 * See also [IrInputField]
 */
data class IrVariable(
    val name: String,
    val defaultValue: IrValue?,
    val type: IrType
    )

data class IrArgument(
    val name: String,
    /**
     * the GQLValue coerced so that for an example, Ints used in Float positions are correctly transformed
     */
    val value: IrValue,
    /**
     * The defaultValue from the GQLArgumentDefinition, coerced
     */
    val defaultValue: IrValue?,
    val type: IrType
)

sealed class IrValue

data class IntIrValue(val value: Int) : IrValue()
data class FloatIrValue(val value: Double) : IrValue()
data class StringIrValue(val value: String) : IrValue()
data class BooleanIrValue(val value: Boolean) : IrValue()
data class EnumIrValue(val value: String) : IrValue()
object NullIrValue : IrValue()
data class ObjectIrValue(val fields: List<Field>) : IrValue() {
  data class Field(val name: String, val value: IrValue)
}

data class ListIrValue(val values: List<IrValue>) : IrValue()
data class VariableIrValue(val name: String) : IrValue()

sealed class IrType {
  abstract val leafName: String
}

data class NonNullIrType(val ofType: IrType) : IrType() {
  override val leafName = ofType.leafName
}

data class ListIrType(val ofType: IrType) : IrType() {
  override val leafName = ofType.leafName
}

sealed class NamedIrType(val name: String) : IrType() {
  override val leafName = name

  override fun hashCode(): Int {
    return name.hashCode()
  }

  /**
   * Ideally we would have data classes here but having `name` as a base property is useful
   * Revisit with sealed interfaces
   */
  override fun equals(other: Any?): Boolean {
    if (other !is NamedIrType) {
      return false
    }
    return name == other.name
  }
}

object StringIrType : NamedIrType("String")
object IntIrType : NamedIrType("Int")
object FloatIrType : NamedIrType("Float")
object BooleanIrType : NamedIrType("Boolean")
object IdIrType : NamedIrType("ID")
class CustomScalarIrType(name: String) : NamedIrType(name)
class EnumIrType(name: String) : NamedIrType(name)
class UnionIrType(name: String) : NamedIrType(name)
class ObjectIrType(name: String) : NamedIrType(name)
class InputObjectIrType(name: String) : NamedIrType(name)
class InterfaceIrType(name: String) : NamedIrType(name)
