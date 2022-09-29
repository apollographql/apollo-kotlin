package com.apollographql.apollo3.compiler.ir

import com.apollographql.apollo3.api.BTerm
import com.apollographql.apollo3.api.BVariable
import com.apollographql.apollo3.api.BooleanExpression
import com.apollographql.apollo3.api.containsPossibleTypes
import com.apollographql.apollo3.ast.GQLType
import com.apollographql.apollo3.compiler.codegen.Identifier.type

/**
 * Intermediate representation (IR)
 *
 * Compared to the GraphQL AST, the IR:
 * - Transforms [com.apollographql.apollo3.ast.GQLField] into [IrProperty] and [IrModel]
 * - moves @include/@skip directives on inline fragments and object fields to their children selections
 * - interprets @deprecated directives
 * - coerces argument values and resolves defaultValue
 * - infers fragment variables
 * - registers used types and fragments
 * - more generally removes all references to the GraphQL AST and "embeds" type definitions/field definitions
 *
 * Additional notes:
 * - In order to ensure reproducibility, prefer using [List] instead of [Set] so that the order is guaranteed
 * - The IR doesn't escape identifiers because different targets might have different escaping rules. The names
 * found in the IR are as found in the GraphQL documents
 *
 */
internal data class Ir(
    val operations: List<IrOperation>,
    val fragments: List<IrFragmentDefinition>,
    val inputObjects: List<IrInputObject>,
    val enums: List<IrEnum>,
    val customScalars: List<IrCustomScalar>,
    val objects: List<IrObject>,
    val unions: List<IrUnion>,
    val interfaces: List<IrInterface>,
    val connectionTypes: List<String>,
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
 * so they are modeled differently in the [Ir]
 */
internal data class IrInputField(
    val name: String,
    val description: String?,
    val deprecationReason: String?,
    val optInFeature: String?,
    val type: IrType,
    val defaultValue: IrValue?,
)

internal data class IrOperation(
    val name: String,
    val operationType: IrOperationType,
    val typeCondition: String,
    val variables: List<IrVariable>,
    val description: String?,
    val selectionSets: List<IrSelectionSet>,
    /**
     * the executableDocument sent to the server
     */
    val sourceWithFragments: String,
    val filePath: String,
    val responseBasedDataModelGroup: IrModelGroup?,
    val dataProperty: IrProperty,
    val dataModelGroup: IrModelGroup,
)

internal data class IrSelectionSet(
    /**
     * a name for this [IrSelectionSet]. This name is unique across all [IrSelectionSet] for a given operation/fragment definition
     */
    val name: String,
    /**
     * true if this is the root selection set for this operation/fragment definition
     */
    val isRoot: Boolean,
    val selections: List<IrSelection>,
)

internal sealed interface IrSelection

internal data class IrField(
    val name: String,
    val alias: String?,
    val type: IrTypeRef,
    val condition: BooleanExpression<BVariable>,
    val arguments: List<IrArgument>,
    val selectionSetName: String?,
) : IrSelection

internal data class IrArgument(
    val name: String,
    val value: IrValue,
    val isKey: Boolean = false,
    val isPagination: Boolean = false,
)

internal sealed interface IrTypeRef
internal data class IrNonNullTypeRef(val ofType: IrTypeRef) : IrTypeRef
internal data class IrListTypeRef(val ofType: IrTypeRef) : IrTypeRef
internal data class IrNamedTypeRef(val name: String) : IrTypeRef

internal data class IrFragment(
    val typeCondition: String,
    val possibleTypes: List<String>,
    val condition: BooleanExpression<BVariable>,
    /**
     * The name of the [IrSelectionSet] that contains the [IrSelection] for this inline fragment
     * or null for fragments spreads (because the [IrSelectionSet] is defined in the fragment
     */
    val selectionSetName: String?,
    /**
     * The name of the fragment for fragment spreads or null for inline fragments
     */
    val name: String?,
) : IrSelection

internal data class IrFragmentDefinition(
    val name: String,
    val description: String?,
    val filePath: String,
    /**
     * Fragments do not have variables per-se (as of writing) but we can infer them from the document
     * Default values will always be null for those
     */
    val variables: List<IrVariable>,
    val typeCondition: String,
    val selectionSets: List<IrSelectionSet>,
    val interfaceModelGroup: IrModelGroup?,
    val dataProperty: IrProperty,
    val dataModelGroup: IrModelGroup,
)

internal sealed class IrOperationType(val typeName: String) {
  val name: String
    get() {
      return when (this) {
        is Query -> "Query"
        is Mutation -> "Mutation"
        is Subscription -> "Subscription"
      }
    }

  class Query(name: String) : IrOperationType(name)
  class Mutation(name: String) : IrOperationType(name)
  class Subscription(name: String) : IrOperationType(name)
}

/**
 * Information about a field that is going to be turned into an [IrProperty]. This merges fields and replaces directives
 * by things that are easier to use from codegen (description, deprecation, etc...)
 *
 * [type] can be used to be resolved to a model. It is made nullable if this field has an `@include` or `@defer` condition
 *
 * - For merged fields, [IrFieldInfo] is the information that is common to all merged fields
 * - For synthetic fields, it is constructed by hand
 *
 * TODO: maybe merge this with [IrProperty]
 */
internal data class IrFieldInfo(
    /**
     * The responseName of this field (or synthetic name)
     */
    val responseName: String,

    /**
     * from the fieldDefinition.
     *
     * It might contain IrModelType that point to generated models
     */
    val type: IrType,

    /**
     * The GraphQL type of the field needed to build the CompiledField
     * null for synthetic fields
     *
     * TODO: CompiledField duplicates "operation_document" so we could certainly remove it (and gqlType too)
     */
    val gqlType: GQLType?,

    /**
     * from the fieldDefinition
     *
     * This can technically differ if the field is implemented on different objects/interfaces.
     * For convenience, we take the value of the first encountered field
     */
    val description: String?,

    /**
     * from the fieldDefinition directives
     *
     * This can technically differ if the field is implemented on different objects/interfaces.
     * For convenience, we take the value of the first enocuntered field
     */
    val deprecationReason: String?,

    /**
     * from the fieldDefinition directives
     */
    val optInFeature: String?,
)

internal sealed class IrAccessor {
  abstract val returnedModelId: String
}

internal data class IrFragmentAccessor(
    val fragmentName: String,
    override val returnedModelId: String,
) : IrAccessor()

internal data class IrSubtypeAccessor(
    val typeSet: TypeSet,
    override val returnedModelId: String,
) : IrAccessor()

/**
 * A class or interface representing a GraphQL object field
 *
 * Monomorphic fields will always be represented by a class while polymorphic fields will involve interfaces
 */
internal data class IrModel(
    val modelName: String,
    /**
     * The path to this field. See [IrModelType] for more details
     */
    val id: String,
    /**
     * The typeSet of this model.
     * Used by the adapters for ordering/making the code look nice but has no runtime impact
     */
    val typeSet: TypeSet,
    val properties: List<IrProperty>,
    /**
     * The possible types
     * Used by the polymorphic adapter to generate the `when` statement that chooses the concrete adapter
     * to delegate to
     */
    val possibleTypes: List<String>,
    val accessors: List<IrAccessor>,
    /**
     * A list of paths to interfaces that the model implements
     */
    val implements: List<String>,
    /**
     * Nested models. Might be empty if the models are flattened
     */
    val modelGroups: List<IrModelGroup>,
    val isInterface: Boolean,
    val isFallback: Boolean,
)

/**
 * @param condition a condition for reading the property
 * @param requiresBuffering true if this property contains synthetic properties and needs to be buffered
 */
internal data class IrProperty(
    val info: IrFieldInfo,
    val override: Boolean,
    val condition: BooleanExpression<BTerm>,
    val requiresBuffering: Boolean,
) {
  /**
   * synthetic properties are special as we need to rewind the reader before reading them
   * They are read in a second pass and are not real json names
   */
  val isSynthetic: Boolean
    get() = info.gqlType == null

  /**
   * whether this field requires a typename to determine if we should parse it or not.
   * This is true for synthetic fragment fields on polymorphic fields
   */
  val requiresTypename: Boolean
    get() = condition.containsPossibleTypes()
}

internal data class IrModelGroup(
    val baseModelId: String,
    val models: List<IrModel>,
)

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

internal data class IrMapProperty(
    val name: String,
    val type: IrType2,
)

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

internal data class IrInterface(
    override val name: String,
    override val targetName: String?,
    val implements: List<String>,
    val keyFields: List<String>,
    val description: String?,
    val deprecationReason: String?,
    val embeddedFields: List<String>,
) : IrSchemaType

internal data class IrUnion(
    override val name: String,
    override val targetName: String?,
    val members: List<String>,
    val description: String?,
    val deprecationReason: String?,
) : IrSchemaType

internal data class IrCustomScalar(
    override val name: String,
    override val targetName: String?,
    val kotlinName: String?, // might be null if no user mapping is provided
    val description: String?,
    val deprecationReason: String?,
) : IrSchemaType {
  val type = IrScalarType(name)
}

/**
 * See also [IrInputField]
 */
internal data class IrVariable(
    val name: String,
    val defaultValue: IrValue?,
    val type: IrType,
)

internal sealed class IrValue

internal data class IrIntValue(val value: Int) : IrValue()
internal data class IrFloatValue(val value: Double) : IrValue()
internal data class IrStringValue(val value: String) : IrValue()
internal data class IrBooleanValue(val value: Boolean) : IrValue()
internal data class IrEnumValue(val value: String) : IrValue()
internal object IrNullValue : IrValue()
internal data class IrObjectValue(val fields: List<Field>) : IrValue() {
  data class Field(val name: String, val value: IrValue)
}

internal data class IrListValue(val values: List<IrValue>) : IrValue()
internal data class IrVariableValue(val name: String) : IrValue()


internal sealed class IrType {
  open fun rawType() = this
}

internal data class IrNonNullType(val ofType: IrType) : IrType() {
  init {
    check(ofType !is IrNonNullType)
  }

  override fun rawType() = ofType.rawType()
}

internal data class IrOptionalType(val ofType: IrType) : IrType() {
  override fun rawType() = ofType.rawType()
}

internal data class IrListType(val ofType: IrType) : IrType() {
  init {
    check(ofType !is IrOptionalType)
  }

  override fun rawType() = ofType.rawType()
}


internal sealed interface IrNamedType {
  val name: String
}

internal data class IrScalarType(override val name: String) : IrType(), IrNamedType
internal data class IrInputObjectType(override val name: String) : IrType(), IrNamedType
internal data class IrEnumType(override val name: String) : IrType(), IrNamedType

/**
 * @param path a unique path identifying a given model.
 *
 * - responseBased: Each dot is a dot in the Json response
 * operationData.$operationName.Data.DroidHero
 * fragmentData.$fragmentName.Data.Hero // interface
 * fragmentData.$fragmentName.Data.OtherHero
 * fragmentData.$fragmentName.Data.DroidHero
 * fragmentData.$fragmentName.Data.HumanHero
 * fragmentData.$fragmentName.Data.HumanHero.CharacterFriend
 * fragmentInterface.$fragmentName.Data.CharacterHero

 * - experimental_operationBasedWithInterfaces:
 * operationData.$operationName.Data.Hero // interface
 * operationData.$operationName.Data.DroidHero.OnDroid.HumanFriend.onHuman
 * operationData.$operationName.Data.OtherHero.Starship
 *
 */
internal data class IrModelType(val path: String) : IrType()

internal const val MODEL_OPERATION_DATA = "operationData"
internal const val MODEL_FRAGMENT_DATA = "fragmentData"
internal const val MODEL_FRAGMENT_INTERFACE = "fragmentInterface"
internal const val MODEL_UNKNOWN = "?"

internal fun IrType.makeOptional(): IrType = IrNonNullType(IrOptionalType(this))
internal fun IrType.makeNullable(): IrType = if (this is IrNonNullType) {
  this.ofType.makeNullable()
} else {
  this
}

internal fun IrType.makeNonNull(): IrType = if (this is IrNonNullType) {
  this
} else {
  IrNonNullType(this)
}

internal fun IrType.isOptional() = (this is IrNonNullType) && (this.ofType is IrOptionalType)

internal fun IrType.makeNonOptional(): IrType {
  return ((this as? IrNonNullType)?.ofType as? IrOptionalType)?.ofType ?: error("$type is not an optional type")
}

