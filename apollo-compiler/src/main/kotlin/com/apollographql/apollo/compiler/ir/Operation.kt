package com.apollographql.apollo.compiler.ir

import com.apollographql.apollo.api.Operation
import com.apollographql.apollo.compiler.SchemaTypeSpecBuilder
import com.squareup.javapoet.TypeSpec
import javax.lang.model.element.Modifier

data class Operation(
    val operationName: String,
    val operationType: String,
    val variables: List<Variable>,
    val source: String,
    val fields: List<Field>,
    val filePath: String,
    val fragmentsReferenced: List<String>,
    val operationId: String

) : CodeGenerator {
  override fun toTypeSpec(context: CodeGenerationContext): TypeSpec =
      SchemaTypeSpecBuilder(
          typeName = DATA_TYPE_NAME,
          fields = fields,
          fragmentSpreads = emptyList(),
          inlineFragments = emptyList(),
          context = context
      )
          .build(Modifier.PUBLIC, Modifier.STATIC)
          .toBuilder()
          .addSuperinterface(Operation.Data::class.java)
          .build()

  fun isMutation() : Boolean {
    return operationType == TYPE_MUTATION
  }

  fun isQuery() : Boolean {
    return operationType == TYPE_QUERY
  }

  companion object {
    val DATA_TYPE_NAME = "Data"
    val TYPE_MUTATION = "mutation"
    val TYPE_QUERY = "query"
  }
}
