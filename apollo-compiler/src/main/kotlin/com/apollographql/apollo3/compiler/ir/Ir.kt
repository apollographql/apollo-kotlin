package com.apollographql.apollo3.compiler.ir

import com.apollographql.apollo3.api.BTerm
import com.apollographql.apollo3.api.BooleanExpression
import com.apollographql.apollo3.ast.GQLFragmentDefinition
import com.apollographql.apollo3.ast.GQLSelection
import com.apollographql.apollo3.ast.Schema

/*
* IR.
*
* Compared to the GraphQL AST, the IR:
* - moves @include/@skip directives on inline fragments and object fields to their children selections
* - interprets @deprecated directives
* - coerces argument values and resolves defaultValue
* - infers fragment variables
* - registers used types and fragments
* - compute the packageName
* - more generally removes all references to the GraphQL AST and "embeds" type definitions/field definitions
*
* @param metadataFragments: the metadata fragments carried over from the previous step so that the final codegen step does not generate them
* @param metadataEnums: the metadata enums carried over from the previous step so that the final codegen step does not generate them
* @param metadataInputObjects: the metadata input objects carried over from the previous step so that the final codegen step does not generate them
*/
data class Ir(
    val operations: List<IrOperation>,
    val fragments: List<IrNamedFragment>,
    val inputObjects: List<IrInputObject>,
    val enums: List<IrEnum>,
    val customScalars: List<IrCustomScalar>,
    val objects: List<IrObject>,
    val unions: List<IrUnion>,
    val interfaces: List<IrInterface>,
    val allFragmentDefinitions: Map<String, GQLFragmentDefinition>,
    val schema: Schema
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
 * Note: [IrInputField],  and [IrVariable] are all very similar since they all share
 * the [com.apollographql.apollo3.ast.GQLInputValueDefinition] type but they also
 * have differences which is why they are different IR models:
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
 */
data class IrOperation(
    val name: String,
    val operationType: IrOperationType,
    val typeCondition: String,
    val variables: List<IrVariable>,
    val description: String?,
    val selections: List<GQLSelection>,
    val sourceWithFragments: String,
    val filePath: String,
    val dataModelGroup: IrModelGroup,
)

data class IrNamedFragment(
    val name: String,
    val description: String?,
    val filePath: String?,
    /**
     * Fragments do not have variables per-se (as of writing) but we can infer them from the document
     * Default values will always be null for those
     */
    val variables: List<IrVariable>,
    val typeCondition: String,
    val selections: List<GQLSelection>,
    val interfaceModelGroup: IrModelGroup?,
    val dataModelGroup: IrModelGroup,
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
    val responseName: String,
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
)

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

/**
 * A Kotlin class or interface representing a GraphQL object field
 */
data class IrModel(
    val modelName: String,
    val id: IrModelId,
    /**
     * The typeSet of this model.
     * Used by the adapters for ordering/making the code look nice
     */
    val typeSet: TypeSet,
    val properties: List<IrProperty>,
    /**
     * The possible types
     * Used by the adapters to generate the polymorphic reading code
     */
    val possibleTypes: Set<String>,
    val accessors: List<IrAccessor>,
    val implements: List<IrModelId>,
    /**
     * Nested models. Might be empty if the models are flattened
     */
    val modelGroups: List<IrModelGroup>,
    val isInterface: Boolean,
    val isFallback: Boolean,
)

/**
 * @param condition a condition for reading the property
 * @param isSynthetic synthetic properties are special as we need to rewind the reader before reading them
 * @param requiresBuffering true if this property contains synthetic properties
 * @param hidden allows to hide a property from the model but still send it to the server and have it in the response
 * This is used for typename for an example
 */
data class IrProperty(
    val info: IrFieldInfo,
    val override: Boolean,
    val condition: BooleanExpression<BTerm>,
    val isSynthetic: Boolean,
    val requiresBuffering: Boolean,
    val hidden: Boolean
)

data class IrModelGroup(
    val baseModelId: IrModelId,
    val models: List<IrModel>,
)

data class IrInputObject(
    val name: String,
    val description: String?,
    val deprecationReason: String?,
    val fields: List<IrInputField>,
)

data class IrObject(
    val name: String,
    val implements: List<String>,
    val description: String?,
    val deprecationReason: String?,
)

data class IrInterface(
    val name: String,
    val implements: List<String>,
    val description: String?,
    val deprecationReason: String?,
)

data class IrUnion(
    val name: String,
    val members: List<String>,
    val description: String?,
    val deprecationReason: String?,
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
  init {
    check(ofType !is IrNonNullType)
  }

  override fun leafType() = ofType.leafType()
}

data class IrOptionalType(val ofType: IrType) : IrType() {
  override fun leafType() = ofType.leafType()
}

data class IrListType(val ofType: IrType) : IrType() {
  init {
    check(ofType !is IrOptionalType)
  }

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

fun IrType.makeOptional(): IrType = IrNonNullType(IrOptionalType(this))
fun IrType.makeNullable(): IrType = if (this is IrNonNullType) {
  this.ofType.makeNullable()
} else {
  this
}
fun IrType.makeNonNull(): IrType = if (this is IrNonNullType) {
  this
} else {
  IrNonNullType(this)
}
fun IrType.isOptional() = (this is IrNonNullType) && (this.ofType is IrOptionalType)

/**
 * A placeholder for compound types until we assign them an id
 */
val IrUnknownModelId = IrModelId(IrModelRoot(IrRootKind.OperationData, "?"), "?")

/**
 * A unique, stable id identifying a model
 */
data class IrModelId(val root: IrModelRoot, val id: String)
data class IrModelRoot(val kind: IrRootKind, val name: String)
enum class IrRootKind {
  FragmentInterface,
  FragmentData,
  OperationData
}