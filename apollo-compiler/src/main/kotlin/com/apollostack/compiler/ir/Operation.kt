package com.apollostack.compiler.ir

import com.apollostack.api.graphql.Operation
import com.apollostack.compiler.SchemaTypeSpecBuilder
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
      typeDeclarations: List<TypeDeclaration>, fragmentsPkgName: String, typesPkgName: String): TypeSpec =
      SchemaTypeSpecBuilder(INTERFACE_TYPE_SPEC_NAME, fields, emptyList(), emptyList(), abstractClass, reservedTypeNames,
          typeDeclarations, fragmentsPkgName, typesPkgName)
          .build(Modifier.PUBLIC, Modifier.STATIC)
          .toBuilder()
          .addSuperinterface(ClassName.get(Operation.Data::class.java))
          .build()

  companion object {
    val INTERFACE_TYPE_SPEC_NAME = "Data"
  }
}
