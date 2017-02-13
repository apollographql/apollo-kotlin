package com.apollographql.android.compiler.ir

import com.apollographql.android.api.graphql.Operation
import com.apollographql.android.compiler.SchemaTypeSpecBuilder
import com.squareup.javapoet.TypeSpec
import javax.lang.model.element.Modifier

data class Operation(
    val operationName: String,
    val operationType: String,
    val variables: List<Variable>,
    val source: String,
    val fields: List<Field>,
    val filePath: String,
    val fragmentsReferenced: List<String>
) : CodeGenerator {
  override fun toTypeSpec(context: CodeGenerationContext): TypeSpec =
      SchemaTypeSpecBuilder(INTERFACE_TYPE_SPEC_NAME, fields, emptyList(), emptyList(), context)
          .build(Modifier.PUBLIC, Modifier.STATIC)
          .toBuilder()
          .addSuperinterface(Operation.Data::class.java)
          .build()

  companion object {
    val INTERFACE_TYPE_SPEC_NAME = "Data"
  }
}
