package com.apollographql.apollo3.compiler.unified

/*
* Unified IR. This builds all the possible field trees. As polymorphic fields are encountered, each field can have multiple
* [IrFieldSet], building a tree where nodes are alternatively [IrField] and [IrFieldSet]
*
* In details, compared to the GraphQL AST, the IR:
* - moves @include/@skip directives on inline fragments and object fields to their children selections
* - interprets @deprecated directives
* - coerces argument values and resolves defaultValue
* - infers fragment variables
* - registers used types and fragments
* - compute the packageName
* - more generally removes all references to the GraphQL AST and "embeds" type definitions/field definitions
*/
data class IntermediateRepresentation(
    val operations: List<IrOperation>,
    val fragments: List<IrNamedFragment>,
    val inputObjects: List<IrInputObject>,
    val enums: List<IrEnum>,
    val customScalars: IrCustomScalars,
)

data class IrEnum(
    val packageName: String,
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

data class IrCustomScalars(
    val packageName: String,
    val customScalars: List<IrCustomScalar>,
)

/**
 * The path to a model. This maps 1:1 with a kotlinpoet [com.squareup.kotlinpoet.TypeName]
 */
data class ModelPath(
    val packageName: String,
    val elements: List<String> = emptyList(),
) {
  operator fun plus(element: String) = copy(elements = elements + element)
}

/**
 * An input field
 *
 * Note: [IrInputField], [IrArgument] and [IrVariable] are all very similar since they all share
 * the [com.apollographql.apollo3.compiler.frontend.GQLInputValueDefinition] type but they also
 * have differences which is why they are different IR models:
 * - [IrArgument] also has a value in addition to the type definition
 * - [IrVariable] doesn't have a description
 */
data class IrInputField(
    val name: String,
    val description: String?,
    val deprecationReason: String?,
    val type: IrType,
    val defaultValue: IrValue?,
    val optional: Boolean,
)

/**
 * @param sourceWithFragments the executableDocument
 */
data class IrOperation(
    val name: String,
    val operationType: IrOperationType,
    val operationId: String,
    val typeCondition: String,
    val variables: List<IrVariable>,
    val description: String?,
    val dataField: IrField,
    val sourceWithFragments: String,
    val packageName: String,
)

data class IrNamedFragment(
    val name: String,
    val description: String?,
    val filePath: String,
    val interfaceField: IrField,
    val dataField: IrField,
    /**
     * Fragments do not have variables per-se but we can infer them from the document
     * Default values will always be null for those
     */
    val variables: List<IrVariable>,
    val typeCondition: String,
    val packageName: String,
)

enum class IrOperationType {
  Query,
  Mutation,
  Subscription
}

data class IrFragmentAccessor(
    val name: String,
    val path: ModelPath,
)

/**
 * A field in the IR tree
 *
 * A field can be:
 * - scalar: [fieldSets] will be empty
 * - monomorphic: [fieldSets] will contain only one [IrFieldSet]
 * - polymorphic: [fieldSets] will contain all possible shapes and interfaces. [interfaces] and [implementations] will be a super set of [fieldSets]
 * with possible "OtherFooBar" [IrFieldSet] to account for cases where we need an implementation with the same name as an interface
 */
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

    // whether this fields needs an override modifier
    val override: Boolean,

    val fragmentAccessors: List<IrFragmentAccessor>,

    // the field set corresponding to the fieldType
    // null for scalar type
    val typeFieldSet: IrFieldSet?,
    /**
     * The [IrFieldSet] independently of interfaces or implementations
     */
    val fieldSets: List<IrFieldSet>,
    /**
     * The [IrFieldSet] to be used as interfaces
     * Might be empty for scalar fields or monomorphic fields
     * For polymorphic fields, this will always contain the field type
     */
    val interfaces: List<IrFieldSet>,
    /**
     * The [IrFieldSet] to be used as implementations
     * Might contain OtherXyz [IrFieldSet] in case a [TypeSet] needs an interface and
     * an implementation at the same time
     * This will only be empty for scalar fields
     */
    val implementations: List<IrFieldSet>,
) {
  val responseName = alias ?: name
}

/**
 * An [IrFieldSet] is a list of fields satisfying some conditions.
 *
 * @param possibleTypes: the possibleTypes that will map to this [IrFieldSet].
 * @param implements: A list of fragment and operation path that this field set will implement
 * @param path: The path up (but not including) to the fieldSet. Use [fullPath] to have everything
 * @param modelName: the name of the model. This is an exception where we format the graphql
 * names instead of calling kotlinNameForXyz(). This is just easier, especially to handle
 * the case where there are "Other" implementations
 */
data class IrFieldSet(
    val path: ModelPath,
    val modelName: String,
    val typeSet: TypeSet,
    val fields: List<IrField>,
    val possibleTypes: Set<String>,
    val implements: Set<ModelPath>,
) {
  val fullPath = path + modelName

  override fun toString(): String {
    return fullPath.elements.joinToString(".")
  }
}

data class IrInputObject(
    val packageName: String,
    val name: String,
    val description: String?,
    val deprecationReason: String?,
    val fields: List<IrInputField>,
)

data class IrCustomScalar(
    val packageName: String,
    val name: String,
    val kotlinName: String?, // might be null if no user mapping is provided
    val description: String?,
    val deprecationReason: String?,
)

/**
 * See also [IrInputField]
 */
data class IrVariable(
    val name: String,
    val defaultValue: IrValue?,
    val type: IrType,
    val optional: Boolean,
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
  open fun leafType() = this
}

data class IrNonNullType(val ofType: IrType) : IrType() {
  override fun leafType() = ofType.leafType()
}

data class IrListType(val ofType: IrType) : IrType() {
  override fun leafType() = ofType.leafType()
}


object IrStringType : IrType()
object IrIntType : IrType()
object IrFloatType : IrType()
object IrBooleanType : IrType()
object IrIdType : IrType()
object IrAnyType : IrType()
class IrCustomScalarType(val customScalar: IrCustomScalar) : IrType()
class IrEnumType(val enum: IrEnum) : IrType()

// InputObjects can have cycles so the inputObject is lazy
class IrInputObjectType(val inputObject: () -> IrInputObject) : IrType()

/**
 * A compound type (objects, unions, interfaces)
 *
 * Compound types are nested and must be resolved with some context so we just record them with a placeholder
 */
object IrCompoundType : IrType()

