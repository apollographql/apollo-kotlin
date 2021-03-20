package com.apollographql.apollo3.compiler.unified

/*
* Unified IR. This builds all the possible field trees. As polymorphic fields are encountered, each field can have multiple
* fieldset, building a tree where node are alternatively [IrField] and [IrFieldSet]
* Also
* - moves @include/@skip directives on inline fragments and object fields to their children selections
* - interprets @deprecated directives
* - coerce argument values and resolves defaultValue
* - infers fragment variables
* - records used types and fragments
* - more generally removes all references to the GraphQL AST and "embeds" type definitions/field definitions
*/
data class IntermediateRepresentation(
    val operations: List<IrOperation>,
    val allNamedFragments: List<IrNamedFragment>,
    val namedFragmentsToGenerate: Set<String>,
    val inputObjects: List<IrInputObject>,
    val enums: List<IrEnum>,
    val customScalars: List<IrCustomScalar>,
)

data class IrEnum(
    val name: String,
    val description: String?,
    val values: List<Value>,
) {
  data class Value(
      val name: String,
      val description: String?,
      val deprecationReason: String?,
  )
}


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
    val defaultValue: IrValue?,
)

/**
 * @param sourceWithFragments the executableDocument
 * @param filePath the path relative to the source roots
 */
data class IrOperation(
    val name: String,
    val operationType: IrOperationType,
    val typeCondition: String,
    val variables: List<IrVariable>,
    val description: String?,
    val dataField: IrField,
    val sourceWithFragments: String,
    val filePath: String,
)

data class IrNamedFragment(
    val name: String,
    val description: String?,
    val filePath: String,
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
  val interfaceFieldSets = fieldSets.filter { it.possibleTypes.isEmpty() }
  val shapesFieldSets = fieldSets.filter { it.possibleTypes.isNotEmpty() }
}

/**
 * An [IrFieldSet] is a list of fields satisfying some conditions.
 * [IrFieldSet] can either represent:
 * - a shape, ie a list of fields that map to at least one concrete type and might appear in a json response
 * - an interface, ie a list of fields common to some shapes but that do not represent a valid json response on their own.
 *
 * The returned interfaces might not be all used. The responsibility of pruning the unused interfaces is left to a later stage of
 * the codegen
 *
 * @param possibleTypes: the possibleTypes that will map to this [IrFieldSet]. If empty, this should map to and Interface
 * @param superTypeSets: the typeSets this [IrFieldSet] inherits from. Only valid for Shapes
 * @param namedFragments: the named fragments this [IrFieldSet] inherits from. Only valid for Shapes
 */
data class IrFieldSet(
    val typeSet: Set<String>,
    val fields: List<IrField>,
    val possibleTypes: Set<String>,
    val superTypeSets: Set<TypeSet>,
    val namedFragments: Set<String>,
)

data class IrInputObject(
    val name: String,
    val description: String?,
    val deprecationReason: String?,
    val fields: List<IrInputField>,
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
    val type: IrType,
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
    val type: IrType,
)

sealed class IrValue

data class IrIntValue(val value: Int) : IrValue()
data class IrFloatValue(val value: Double) : IrValue()
data class IrStringValue(val value: String) : IrValue()
data class IrBooleanValue(val value: Boolean) : IrValue()
data class IrEnumValue(val value: String) : IrValue()
object IrNullValue : IrValue()
data class IrObjectValue(val fields: List<Field>) : IrValue() {
  data class Field(val name: String, val value: IrValue)
}

data class IrListValue(val values: List<IrValue>) : IrValue()
data class IrVariableValue(val name: String) : IrValue()

sealed class IrType {
  abstract val leafName: String
}

data class IrNonNullType(val ofType: IrType) : IrType() {
  override val leafName = ofType.leafName
}

data class IrListType(val ofType: IrType) : IrType() {
  override val leafName = ofType.leafName
}

sealed class IrNamedType(val name: String) : IrType() {
  override val leafName = name

  override fun hashCode(): Int {
    return name.hashCode()
  }

  /**
   * Ideally we would have data classes here but having `name` as a base property is useful
   * Revisit with sealed interfaces
   */
  override fun equals(other: Any?): Boolean {
    if (other !is IrNamedType) {
      return false
    }
    return name == other.name
  }
}

object IrStringType : IrNamedType("String")
object IrIntType : IrNamedType("Int")
object IrFloatType : IrNamedType("Float")
object IrBooleanType : IrNamedType("Boolean")
object IrIdType : IrNamedType("ID")
class IrCustomScalarType(name: String) : IrNamedType(name)
class IrEnumType(name: String) : IrNamedType(name)
class IrUnionType(name: String) : IrNamedType(name)
class IrObjectType(name: String) : IrNamedType(name)
class IrInputObjectType(name: String) : IrNamedType(name)
class IrInterfaceType(name: String) : IrNamedType(name)
