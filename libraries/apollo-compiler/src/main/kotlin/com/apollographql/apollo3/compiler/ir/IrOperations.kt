package com.apollographql.apollo3.compiler.ir

import com.apollographql.apollo3.ast.GQLFragmentDefinition
import com.apollographql.apollo3.ast.GQLType
import com.apollographql.apollo3.compiler.BooleanExpressionSerializer
import com.apollographql.apollo3.compiler.GQLFragmentDefinitionSerializer
import com.apollographql.apollo3.compiler.GQLTypeSerializer
import kotlinx.serialization.Contextual
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.okio.decodeFromBufferedSource
import kotlinx.serialization.json.okio.encodeToBufferedSink
import okio.buffer
import okio.sink
import okio.source
import java.io.File

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
 */
@Serializable
internal data class DefaultIrOperations(
    val operations: List<IrOperation>,
    val fragments: List<IrFragmentDefinition>,
    override val usedFields: Map<String, Set<String>>,

    val flattenModels: Boolean,
    val decapitalizeFields: Boolean,
    val generateDataBuilders: Boolean,

    override val fragmentDefinitions: List<@Serializable(with = GQLFragmentDefinitionSerializer::class) GQLFragmentDefinition>,
) : IrOperations

interface IrOperations {
  val fragmentDefinitions: List<GQLFragmentDefinition>
  val usedFields: Map<String, Set<String>>
}

@Serializable
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

@Serializable
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

@Serializable
internal sealed interface IrSelection

@Serializable
@SerialName("field")
internal data class IrField(
    val name: String,
    val alias: String?,
    val type: IrTypeRef,
    @Serializable(with = BooleanExpressionSerializer::class)
    val condition: BooleanExpression<@Contextual BVariable>,
    val arguments: List<IrArgument>,
    val selectionSetName: String?,
) : IrSelection

@Serializable
internal data class IrArgument(
    val name: String,
    val value: IrValue,
    val isKey: Boolean = false,
    val isPagination: Boolean = false,
)

@Serializable
internal sealed interface IrTypeRef
@Serializable
@SerialName("nonnull")
internal data class IrNonNullTypeRef(val ofType: IrTypeRef) : IrTypeRef
@Serializable
@SerialName("list")
internal data class IrListTypeRef(val ofType: IrTypeRef) : IrTypeRef
@Serializable
@SerialName("named")
internal data class IrNamedTypeRef(val name: String) : IrTypeRef

@Serializable
@SerialName("fragment")
internal data class IrFragment(
    val typeCondition: String,
    val possibleTypes: List<String>,
    @Serializable(with = BooleanExpressionSerializer::class)
    val condition: BooleanExpression<@Contextual BVariable>,
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

@Serializable
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
    val source: String,
    /**
     * Whether the type condition is an interface or an enum.
     * In that case, the data builder need to require __typename
     */
    val isTypeConditionAbstract: Boolean
)

@Serializable
internal sealed interface IrOperationType {
  val typeName: String

  val name: String
    get() {
      return when (this) {
        is Query -> "Query"
        is Mutation -> "Mutation"
        is Subscription -> "Subscription"
      }
    }

  @Serializable
  @SerialName("query")
  class Query(override val typeName: String) : IrOperationType
  @Serializable
  @SerialName("mutation")
  class Mutation(override val typeName: String) : IrOperationType
  @Serializable
  @SerialName("subscription")
  class Subscription(override val typeName: String) : IrOperationType
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
@Serializable
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
     * The GraphQL type of the field needed to build the CompiledField or null for synthetic fields
     *
     * TODO: CompiledField duplicates "operation_document" so we could certainly remove it (and gqlType too)
     */
    @Serializable(with = GQLTypeSerializer::class)
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
     * For convenience, we take the value of the first encountered field
     */
    val deprecationReason: String?,

    /**
     * from the fieldDefinition directives
     */
    val optInFeature: String?,
)

@Serializable
internal sealed class IrAccessor {
  abstract val returnedModelId: String
}

@Serializable
@SerialName("fragment")
internal data class IrFragmentAccessor(
    val fragmentName: String,
    override val returnedModelId: String,
) : IrAccessor()

@Serializable
@SerialName("subtype")
internal data class IrSubtypeAccessor(
    val typeSet: TypeSet,
    override val returnedModelId: String,
) : IrAccessor()

/**
 * A class or interface representing a GraphQL object field
 *
 * Monomorphic fields will always be represented by a class while polymorphic fields will involve interfaces
 */
@Serializable
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
@Serializable
internal data class IrProperty(
    val info: IrFieldInfo,
    val override: Boolean,
    @Serializable(with = BooleanExpressionSerializer::class)
    val condition: BooleanExpression<@Contextual BTerm>,
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

@Serializable
internal data class IrModelGroup(
    val baseModelId: String,
    val models: List<IrModel>,
)

@Serializable
internal data class IrVariable(
    val name: String,
    val defaultValue: IrValue?,
    val type: IrType,
)

private val json = Json { classDiscriminator = "#class" }

@OptIn(ExperimentalSerializationApi::class)
fun IrOperations.writeTo(file: File) {
  file.sink().buffer().use {
    json.encodeToBufferedSink(this as DefaultIrOperations, it)
  }
}

@OptIn(ExperimentalSerializationApi::class)
fun File.toIrOperations(): IrOperations {
  return source().buffer().use {
    json.decodeFromBufferedSource<DefaultIrOperations>(it)
  }
}
