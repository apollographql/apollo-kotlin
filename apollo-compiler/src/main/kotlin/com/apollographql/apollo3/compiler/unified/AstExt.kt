package com.apollographql.apollo3.compiler.unified

import com.apollographql.apollo3.compiler.backend.ast.CodeGenerationAst

typealias ModelPath = List<String>

fun modelName(typeSet: TypeSet, responseName: String): String {
  return (typeSet.sorted() + responseName).map { it.capitalize() }.joinToString("")
}

internal data class AstExtField(
    val name: String,
    val type: CodeGenerationAst.FieldType,
    val override: Boolean
)

internal interface AstExtModel {
  /**
   * The path to the model. We need that from codegen to be able to create references to this model
   */
  val path: ModelPath
  val description: String
  val deprecationReason: String?
  val fields: List<AstExtField>
  val nestedModels: List<AstExtModel>
}

internal data class AstExtInterface(
    override val path: ModelPath,
    override val description: String,
    override val deprecationReason: String?,
    override val fields: List<AstExtField>,
    override val nestedModels: List<AstExtInterface>,
) : AstExtModel

internal data class AstExtImplementation(
    override val path: ModelPath,
    override val description: String,
    override val deprecationReason: String?,
    override val fields: List<AstExtField>,
    override val nestedModels: List<AstExtImplementation>,
    /**
     * The list of implemented interfaces ordered by depth (same depth goes first)
     */
    val implements: List<AstExtImplementation>,
) : AstExtModel

interface AstExtAdapter

internal data class AstExtMonomorphicFieldAdapter(
    val name: String,
    val fields: List<CodeGenerationAst.Field>,
): AstExtAdapter

internal data class AstExtPolymorphicFieldAdapter(
    val name: String,
    val shapes: List<AstExtShapeAdapter>,
): AstExtAdapter

internal data class AstExtShapeAdapter(
    val name: String,
    val possibleTypes: PossibleTypes,
    val fields: List<CodeGenerationAst.Field>,
): AstExtAdapter

internal data class AstExtOperation(
    val name: String,
    val type: Type,
    val filePath: String,
    val description: String,
    val operationId: String,
    val operationDocument: String,
    val variables: List<CodeGenerationAst.InputField>,
    val dataImplementation: AstExtImplementation?
) {
  enum class Type {
    QUERY, MUTATION, SUBSCRIPTION
  }
}

internal data class AstExtFragmentInterfaces(
    val interfaces: List<AstExtInterface>
)

internal data class AstExtFragmentImplementation(
    val name: String,
    val packageName: String,
    val description: String,
    val variables: List<CodeGenerationAst.InputField>,
    val dataImplementation: AstExtImplementation?
) {
}