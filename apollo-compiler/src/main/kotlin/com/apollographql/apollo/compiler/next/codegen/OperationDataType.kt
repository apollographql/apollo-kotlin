package com.apollographql.apollo.compiler.next.codegen

import com.apollographql.apollo.api.Operation
import com.apollographql.apollo.compiler.applyIf
import com.apollographql.apollo.compiler.next.ast.CodeGenerationAst
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.TypeSpec

internal fun CodeGenerationAst.OperationDataType.toOperationDataTypeSpec(): TypeSpec {
  val dataType = checkNotNull(nestedTypes[rootType]) {
    "Failed to resolve operation root data type"
  }
  return TypeSpec
      .classBuilder(dataType.name)
      .addModifiers(KModifier.DATA)
      .addSuperinterface(Operation.Data::class)
      .applyIf(dataType.description.isNotBlank()) { addKdoc("%L\n", dataType.description) }
      .primaryConstructor(FunSpec.constructorBuilder()
          .addParameters(dataType.fields.map { field ->
            val typeName = field.type.asTypeName()
            ParameterSpec.builder(
                name = field.name,
                type = if (field.type.nullable) typeName.copy(nullable = true) else typeName
            ).build()
          })
          .build()
      )
      .addProperties(dataType.fields.map { field -> field.asPropertySpec(initializer = CodeBlock.of(field.name)) })
      .addType(TypeSpec.companionObjectBuilder()
          .addProperty(responseFieldsPropertySpec(dataType.fields))
          .addFunction(dataType.fields.toMapperFun(ClassName("", dataType.name)))
          .addFunction(ClassName("", dataType.name).createMapperFun())
          .build()
      )
      .addFunction(dataType.fields.marshallerFunSpec(override = true, thisRef = dataType.name))
      .build()
}
