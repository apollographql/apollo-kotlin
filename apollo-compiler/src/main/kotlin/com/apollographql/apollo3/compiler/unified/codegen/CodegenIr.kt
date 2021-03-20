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
    val operations: List<CgOperation>,
    val fragments: List<CgFragment>,
    val inputTypes: List<CgInputObject>,
    val enumTypes: List<CgEnum>,
    val customScalars: CgCustomScalars,
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

internal data class CgCustomScalars(
    val names: List<String>,
)

internal data class CgEnum(
    val name: String,
    val description: String?,
    val values: List<Value>,
) {
  internal data class Value(
      val name: String,
      val description: String?,
      val deprecationReason: String?,
  )
}


internal data class CgInputObject(
    val name: String,
    val description: String?,
    val deprecationReason: String?,
    val inputFields: List<CgProperty>,
)

internal data class CgProperty(
    val name: String,
    val description: String?,
    val deprecationReason: String?,
    val type: CgType,
    val override: Boolean,
)

internal interface CgModel {
  /**
   * The path to the model. We need that from codegen to be able to create references to this model
   */
  val path: ModelPath
  val description: String?
  val deprecationReason: String?
  val properties: List<CgProperty>
  val nestedModels: List<CgModel>
}

internal data class CgInterface(
    override val path: ModelPath,
    override val description: String?,
    override val deprecationReason: String?,
    override val properties: List<CgProperty>,
    override val nestedModels: List<CgInterface>,
) : CgModel

internal data class CgImplementation(
    override val path: ModelPath,
    override val description: String?,
    override val deprecationReason: String?,
    override val properties: List<CgProperty>,
    override val nestedModels: List<CgImplementation>,
    /**
     * The list of implemented interfaces ordered by depth (same depth goes first)
     * a list of [CgOperationModelType] or [CgFragmentModelType]
     */
    val implements: List<CgType>,
) : CgModel

interface CgAdapter

internal data class CgAdaptedField(
    val name: String,
    val type: CgType,
)

internal data class CgMonomorphicAdapter(
    val name: String,
    val adaptedFields: List<CgAdaptedField>,
) : CgAdapter

internal data class CgPolymorphicAdapter(
    val name: String,
    val shapes: List<CgShapeAdapter>,
) : CgAdapter

internal data class CgShapeAdapter(
    val name: String,
    val possibleTypes: PossibleTypes,
    val adaptedFields: List<CgAdaptedField>,
) : CgAdapter

internal data class CgOperation(
    val name: String,
    val filePath: String,
    val description: String?,
    val variables: List<CgVariable>,
    val dataInterfaces: List<CgInterface>,
    val dataImplementations: List<CgImplementation>,
    val variablesAdapter: CgAdapter,
    val dataAdapter: CgAdapter,

    val operationId: String,
    val operationDocument: String,
    val operationType: OperationType,
) {
  enum class OperationType {
    QUERY, MUTATION, SUBSCRIPTION
  }
}

internal data class CgFragment(
    val name: String,
    val filePath: String,
    val description: String?,
    val variables: List<CgVariable>,
    val dataInterfaces: List<CgInterface>,
    val dataImplementations: List<CgImplementation>,
    val variablesAdapter: CgAdapter,
    val dataAdapter: CgAdapter,
)


sealed class CgType

class CgNullableType(val ofType: CgType) : CgType()

/**
 * For input values, we support wrapping them in [Input] to allow differentiating between null and absent
 */
class CgOptionalType(val ofType: CgType) : CgType()
class CgListType(val ofType: CgType) : CgType()
class CgStringType : CgType()
class CgFloatType : CgType()
class CgIntType : CgType()
class CgBooleanType : CgType()

class CgCustomScalarType(val name: String) : CgType()
class CgEnumType(val name: String) : CgType()

fun CgType.optional(optional: Boolean) = when {
  optional && this !is CgOptionalType -> CgOptionalType(this)
  !optional && this is CgOptionalType -> ofType
  else -> this
}

fun CgType.nullable(nullable: Boolean) = when {
  nullable && this !is CgNullableType -> CgNullableType(this)
  !nullable && this is CgNullableType -> ofType
  else -> this
}

class CgInputObjectType(val name: String) : CgType()
class CgModelType(modelPath: ModelPath) : CgType()

internal data class CgVariable(
    val name: String,
    val type: CgType,
)
