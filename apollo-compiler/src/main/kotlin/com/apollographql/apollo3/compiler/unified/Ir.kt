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

data class PathElement(
    val typeSet: TypeSet,
    val fieldType: String,
    val responseName: String,
)

data class ModelPath(
    val packageName: String,
    val root: Root,
    val elements: List<PathElement> = emptyList(),
) {
  operator fun plus(element: PathElement) = copy(elements = elements + element)

  sealed class Root {
    class Operation(val name: String): Root()
    class Fragment(val name: String): Root()
  }
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
    val optional: Boolean,
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
    val packageName: String,
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
    val packageName: String,
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
    // whether this fields needs an override modifier
    val override: Boolean,
    // empty for a scalar field
    val fieldSets: List<IrFieldSet>,
) {
  val responseName = alias ?: name
  val baseFieldSet = fieldSets.firstOrNull { it.typeSet.size == 1 }
  val modelPath = baseFieldSet?.fullPath
}

/**
 * An [IrFieldSet] is a list of fields satisfying some conditions.
 *
 * @param possibleTypes: the possibleTypes that will map to this [IrFieldSet].
 * @param implements: A list of fragment and operation path that this field set will implement
 */
data class IrFieldSet(
    val path: ModelPath,
    val responseName: String,
    val typeSet: Set<String>,
    val fieldType: String,
    val fields: List<IrField>,
    val possibleTypes: Set<String>,
    val implements: Set<ModelPath>,
) {
  val fullPath = (path + PathElement(typeSet, fieldType, responseName))

  private fun modelName(typeSet: TypeSet, responseName: String): String {
    return (typeSet.sorted() + responseName).map { it.capitalize() }.joinToString("")
  }

  override fun toString(): String {
    return fullPath.elements.map { modelName(it.typeSet - it.fieldType, it.responseName) }
        .map { it.capitalize() }
        .joinToString(".")
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
    val name: String,
    val kotlinName: String,
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
  abstract val leafType: IrNamedType
}

data class IrNonNullType(val ofType: IrType) : IrType() {
  override val leafType = ofType.leafType
}

data class IrListType(val ofType: IrType) : IrType() {
  override val leafType = ofType.leafType
}

sealed class IrNamedType(val name: String) : IrType() {
  override val leafType = this

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
object IrAnyType : IrNamedType("Any")
class IrCustomScalarType(name: String, val kotlinName: String, val packageName: String) : IrNamedType(name)
class IrEnumType(name: String, val packageName: String) : IrNamedType(name)
class IrInputObjectType(name: String, val packageName: String) : IrNamedType(name)
class IrCompoundType(name: String, val modelPath: ModelPath) : IrNamedType(name)

