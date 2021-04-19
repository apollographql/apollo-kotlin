package com.apollographql.apollo3.compiler.unified.ir

import com.apollographql.apollo3.compiler.MetadataFragment

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
*
* @param metadataFragments: the metadata fragments carried over from the previous step. This is needed by the final codegen step
* to retrieve the package name if any
*/
data class IntermediateRepresentation(
    val operations: List<IrOperation>,
    val fragments: List<IrNamedFragment>,
    val inputObjects: List<IrInputObject>,
    val enums: List<IrEnum>,
    val customScalars: List<IrCustomScalar>,
    val metadataFragments: List<MetadataFragment>,
    val metadataEnums: Set<String>,
    val metadataInputObjects: Set<String>,
    val metadataSchema: Boolean
)

data class IrEnum(
    val name: String,
    val description: String?,
    val values: List<Value>,
) {
  val type = IrEnumType(name)

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
    val typeCondition: String,
    val variables: List<IrVariable>,
    val description: String?,
    val field: IrField,
    val modelGroups: List<IrModelGroup>,
    val sourceWithFragments: String,
    val filePath: String,
    val dataModelId: IrModelId
) {
}

data class IrNamedFragment(
    val name: String,
    val description: String?,
    val filePath: String?,
    /**
     * Fragments do not have variables per-se but we can infer them from the document
     * Default values will always be null for those
     */
    val variables: List<IrVariable>,
    val typeCondition: String,
    val field: IrField,
    val interfaceModelGroups: List<IrModelGroup>,
    val implementationModelGroups: List<IrModelGroup>,
    val interfaceId: IrModelId,
    val implementationId: IrModelId,
)

enum class IrOperationType {
  Query,
  Mutation,
  Subscription
}

/**
 * When merging fields, [IrFieldInfo] is the information that is common to all merged fields
 */
data class IrFieldInfo(
    val name: String,
    val alias: String?,
    val arguments: List<IrArgument>,
    /**
     * from the fieldDefinition
     * This can technically differ between different merged fields. For convenience, we take the first one
     */
    val description: String?,
    /**
     * from the fieldDefinition
     */
    val type: IrType,
    /**
     * from the fieldDefinition directives
     */
    val deprecationReason: String?,
    /**
     * The name of the rawType, without the NotNull/List decorations
     * When selections is not empty, this is the type condition for these selections
     */
    val rawTypeName: String,
) {
  val responseName = alias ?: name
}

sealed class IrAccessor {
  abstract val returnedModelId: IrModelId
}

data class IrFragmentAccessor(
    val fragmentName: String,
    override val returnedModelId: IrModelId,
) : IrAccessor()

data class IrSubtypeAccessor(
    val typeSet: TypeSet,
    override val returnedModelId: IrModelId,
) : IrAccessor()

data class IrModel(
    val modelName: String,
    val id: IrModelId,
    val typeSet: TypeSet, // Used by adapters to lookup the fallback model
    val properties: List<IrProperty>,
    val possibleTypes: Set<String>,
    val accessors: List<IrAccessor>,
    val implements: List<IrModelId>,
    val modelGroups: List<IrModelGroup>,
    val isInterface: Boolean,
    val isBase: Boolean,
    val isFallback: Boolean
) {
}

data class IrProperty(
    val info: IrFieldInfo,
    val override: Boolean,
    val condition: BooleanExpression,
)

data class IrModelGroup(
    val baseModelId: IrModelId,
    val models: List<IrModel>
)
/**
 * A field in the IR tree
 */
data class IrField(
    val info: IrFieldInfo,
    val condition: BooleanExpression,
    val fieldSets: List<IrFieldSet>,
    val fragments: Set<String>,
)

/**
 * An [IrFieldSet] is a list of fields satisfying some conditions.
 *
 * @param possibleTypes: the possibleTypes that will map to this [IrFieldSet].
 */
data class IrFieldSet(
    val typeSet: TypeSet,
    val fields: List<IrField>,
    val possibleTypes: Set<String>,
)

data class IrInputObject(
    val name: String,
    val description: String?,
    val deprecationReason: String?,
    val fields: List<IrInputField>,
)

data class IrCustomScalar(
    val name: String,
    val kotlinName: String?, // might be null if no user mapping is provided
    val description: String?,
    val deprecationReason: String?,
) {
  val type = IrCustomScalarType(name)
}

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

data class IrCustomScalarType(val name: String) : IrType()
data class IrInputObjectType(val name: String) : IrType()
data class IrEnumType(val name: String) : IrType()
data class IrModelType(val id: IrModelId) : IrType()

/**
 * A placeholder for compound types until we assign them an id
 */
val IrUnknownModelId = IrModelId(IrModelRoot(IrRootKind.Operation, "?"), "?")

data class IrModelId(val root: IrModelRoot, val id: String)
data class IrModelRoot(val kind: IrRootKind, val name: String)
enum class IrRootKind {
  FragmentInterface,
  FragmentImplementation,
  Operation
}