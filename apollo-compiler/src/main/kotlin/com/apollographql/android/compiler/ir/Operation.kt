package com.apollographql.android.compiler.ir

import com.apollographql.android.api.graphql.Operation
import com.apollographql.android.compiler.SchemaTypeSpecBuilder
import com.squareup.javapoet.ClassName
import com.squareup.javapoet.TypeSpec
import javax.lang.model.element.Modifier

data class Operation(
    val operationName: String,
    val operationType: String,
    val variables: List<Variable>,
    val source: String,
    val fields: List<Field>,
    val filePath: String
) : CodeGenerator {
  override fun toTypeSpec(abstractClass: Boolean, reservedTypeNames: List<String>,
      typeDeclarations: List<TypeDeclaration>, fragmentsPackage: String, typesPackage: String): TypeSpec =
      SchemaTypeSpecBuilder(INTERFACE_TYPE_SPEC_NAME, fields, emptyList(), emptyList(), abstractClass, reservedTypeNames,
          typeDeclarations, fragmentsPackage, typesPackage)
          .build(Modifier.PUBLIC, Modifier.STATIC)
          .toBuilder()
          .addSuperinterface(ClassName.get(Operation.Data::class.java))
          .build()

  companion object {
    val INTERFACE_TYPE_SPEC_NAME = "Data"
  }
}
