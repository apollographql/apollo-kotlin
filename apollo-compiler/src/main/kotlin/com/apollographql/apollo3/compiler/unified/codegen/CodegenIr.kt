package com.apollographql.apollo3.compiler.unified.codegen

import com.apollographql.apollo3.compiler.backend.ast.CodeGenerationAst
import com.apollographql.apollo3.compiler.unified.PossibleTypes
import com.apollographql.apollo3.compiler.unified.TypeSet

/**
 * A representation that matches the final kotlin code. It computes the inheritance between the different models and override flags
 * It isn't bound to KotlinPoet or escape any identifier so that it could potentially be used for other languages although for now
 * Kotlin remains the main target.
 * All the names are GraphQL names except the model names that are pre-computed for convenience
 * Also it doesn't know anything about paths, this is left to the final stage. This allows to easily change where we write files. For this
 * reason, Model references contain the filePath
 *
 * The different properties represent different logcial units.
 */
internal data class CodegenIr(
    val operations: List<CGOperation>,
    val fragments: List<CGFragment>,
    val inputTypes: List<CGInputObject>,
    val enumTypes: List<CGEnum>,
    val customScalars: CGCustomScalars,
)

data class PathElement(
    val typeSet: TypeSet,
    val responseName: String,
)

data class ModelPath(val fileName: String, val root: Root, val elements: List<PathElement>) {
  constructor(fileName: String, root: Root, vararg elements: PathElement) : this(fileName, root, elements.toList())

  operator fun plus(element: PathElement) = copy(elements = elements + element)

  enum class Root {
    Operation,
    Fragment
  }
}

fun modelName(typeSet: TypeSet, responseName: String): String {
  return (typeSet.sorted() + responseName).map { it.capitalize() }.joinToString("")
}

internal data class CGCustomScalars(
    val names: List<String>,
)


internal data class CGEnum(
    val name: String,
    val description: String?,
    val values: List<CGEnumValue>,
)

internal data class CGEnumValue(
    val name: String,
    val description: String?,
    val deprecationReason: String?,
)

internal data class CGInputObject(
    val name: String,
    val description: String?,
    val deprecationReason: String?,
    val inputFields: List<CGProperty>,
)

internal data class CGProperty(
    val name: String,
    val description: String?,
    val deprecationReason: String?,
    val type: CGType,
    val override: Boolean,
)

internal interface CGModel {
  /**
   * The path to the model. We need that from codegen to be able to create references to this model
   */
  val path: ModelPath
  val description: String?
  val deprecationReason: String?
  val properties: List<CGProperty>
  val nestedModels: List<CGModel>
}

internal data class CGInterface(
    override val path: ModelPath,
    override val description: String?,
    override val deprecationReason: String?,
    override val properties: List<CGProperty>,
    override val nestedModels: List<CGInterface>,
) : CGModel

internal data class CGImplementation(
    override val path: ModelPath,
    override val description: String?,
    override val deprecationReason: String?,
    override val properties: List<CGProperty>,
    override val nestedModels: List<CGImplementation>,
    /**
     * The list of implemented interfaces ordered by depth (same depth goes first)
     * a list of [CGOperationModelType] or [CGFragmentModelType]
     */
    val implements: List<CGType>,
) : CGModel

interface CGAdapter

internal data class CGAdaptedField(
    val name: String,
    val type: CGType,
)

internal data class CGMonomorphicAdapter(
    val name: String,
    val adaptedFields: List<CGAdaptedField>,
) : CGAdapter

internal data class CGPolymorphicAdapter(
    val name: String,
    val shapes: List<CGShapeAdapter>,
) : CGAdapter

internal data class CGShapeAdapter(
    val name: String,
    val possibleTypes: PossibleTypes,
    val adaptedFields: List<CGAdaptedField>,
) : CGAdapter

internal data class CGOperation(
    val name: String,
    val filePath: String,
    val description: String?,
    val variables: List<CGVariable>,
    val dataInterfaces: List<CGInterface>,
    val dataImplementations: List<CGImplementation>,
    val variablesAdapter: CGAdapter,
    val dataAdapter: CGAdapter,

    val operationId: String,
    val operationDocument: String,
    val operationType: OperationType,
) {
  enum class OperationType {
    QUERY, MUTATION, SUBSCRIPTION
  }
}

internal data class CGFragment(
    val name: String,
    val filePath: String,
    val description: String?,
    val variables: List<CGVariable>,
    val dataInterfaces: List<CGInterface>,
    val dataImplementations: List<CGImplementation>,
    val variablesAdapter: CGAdapter,
    val dataAdapter: CGAdapter,
)


sealed class CGType

class CGNullableType(val ofType: CGType) : CGType()

/**
 * For input values, we support wrapping them in [Input] to allow differentiating between null and absent
 */
class CGOptionalType(val ofType: CGType) : CGType()
class CGListType(val ofType: CGType) : CGType()
class CGStringType : CGType()
class CGFloatType : CGType()
class CGIntType : CGType()
class CGBooleanType : CGType()

class CGCustomScalarType(val name: String) : CGType()
class CGEnumType(val name: String) : CGType()

fun CGType.optional(optional: Boolean) = when {
  optional && this !is CGOptionalType -> CGOptionalType(this)
  !optional && this is CGOptionalType -> ofType
  else -> this
}

fun CGType.nullable(nullable: Boolean) = when {
  nullable && this !is CGNullableType -> CGNullableType(this)
  !nullable && this is CGNullableType -> ofType
  else -> this
}

class CGInputObjectType(val name: String) : CGType()
class CGModelType(modelPath: ModelPath) : CGType()

internal data class CGVariable(
    val name: String,
    val type: CGType,
)
