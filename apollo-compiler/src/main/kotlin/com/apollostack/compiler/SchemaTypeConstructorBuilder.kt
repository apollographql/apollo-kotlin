package com.apollostack.compiler

import com.apollostack.compiler.ir.Field
import com.apollostack.compiler.ir.InlineFragment
import com.squareup.javapoet.*
import javax.lang.model.element.Modifier

class SchemaTypeConstructorBuilder(
    schemaTypeName: String,
    val fields: List<Field>,
    val fragmentSpreads: List<String>,
    val inlineFragments: List<InlineFragment>
) {
  private val schemaClassName = ClassName.get("", schemaTypeName)
  private val hasFragments = inlineFragments.isNotEmpty() || fragmentSpreads.isNotEmpty()

  fun build(): MethodSpec = MethodSpec
      .constructorBuilder()
      .addModifiers(Modifier.PUBLIC)
      .addParameter(READER_PARAM_SPEC)
      .addException(ClassNames.IO_EXCEPTION)
      .addCode(constructorCode())
      .build()

  private fun constructorCode(): CodeBlock = CodeBlock
      .builder()
      .add(responseReadStatement())
      .indent()
      .add(valueHandlerStatement())
      .add(",\n")
      .add(responseFieldFactoryStatements())
      .unindent()
      .add("\n);\n")
      .build()

  private fun responseReadStatement(): CodeBlock = CodeBlock
      .builder()
      .add(
          if (hasFragments) {
            "\$L.toBufferedReader().read(\n"
          } else {
            "\$L.read(\n"
          },
          PARAM_READER)
      .build()

  private fun valueHandlerStatement(): CodeBlock = CodeBlock
      .builder()
      .add("new \$T() {\n", ClassNames.API_RESPONSE_VALUE_HANDLER)
      .indent()
      .beginControlFlow("@Override public void handle(int fieldIndex, Object value)")
      .add(valueHandlerSwitchStatement())
      .endControlFlow()
      .unindent()
      .add("}")
      .build()

  private fun valueHandlerSwitchStatement(): CodeBlock = CodeBlock
      .builder()
      .beginControlFlow("switch (fieldIndex)")
      .add(fields
          .mapIndexed { i, field -> fieldValueCaseStatement(field, i) }
          .fold(CodeBlock.builder(), CodeBlock.Builder::add)
          .build())
      .add(valueHandlerFragmentsCaseStatement(fields.size))
      .endControlFlow()
      .build()

  private fun fieldValueCaseStatement(field: Field, index: Int): CodeBlock {
    val fieldSpec = field.fieldSpec()
    val fieldRawType = fieldSpec.type.withoutAnnotations()
    return CodeBlock.builder()
        .beginControlFlow("case $index:")
        .addStatement("\$T.this.\$L = (\$T) value", schemaClassName, fieldSpec.name, fieldRawType)
        .addStatement("break")
        .endControlFlow()
        .build()
  }

  private fun valueHandlerFragmentsCaseStatement(index: Int): CodeBlock {
    if (hasFragments) {
      return CodeBlock
          .builder()
          .beginControlFlow("case $index:")
          .addStatement("\$T \$L = (\$T) value", ClassNames.STRING, PARAM_TYPE_NAME, ClassNames.STRING)
          .add(inlineFragments
              .map { inlineFragmentInitStatement(it) }
              .fold(CodeBlock.builder(), CodeBlock.Builder::add)
              .build())
          .add(fragmentsInitStatement())
          .addStatement("break")
          .endControlFlow()
          .build()
    } else {
      return CodeBlock.of("")
    }
  }

  private fun inlineFragmentInitStatement(fragment: InlineFragment): CodeBlock {
    val fieldSpec = fragment.fieldSpec()
    val fieldRawType = fieldSpec.type.withoutAnnotations()
    return CodeBlock.builder()
        .beginControlFlow("if (\$L.equals(\$S))", PARAM_TYPE_NAME, fragment.typeCondition)
        .addStatement("\$T.this.\$L = new \$T(\$L)", schemaClassName, fieldSpec.name, fieldRawType, PARAM_READER)
        .endControlFlow()
        .build()
  }

  private fun fragmentsInitStatement(): CodeBlock {
    if (fragmentSpreads.isNotEmpty()) {
      return CodeBlock.builder()
          .addStatement("\$T.this.\$L = new \$L(\$L, \$L)", schemaClassName,
              SchemaTypeSpecBuilder.FRAGMENTS_INTERFACE_NAME.decapitalize(),
              SchemaTypeSpecBuilder.FRAGMENTS_INTERFACE_NAME, PARAM_READER, PARAM_TYPE_NAME)
          .build()
    } else {
      return CodeBlock.of("")
    }
  }

  private fun responseFieldFactoryStatements(): CodeBlock {
    val typeNameFieldStatement = if (hasFragments) {
      CodeBlock.of("\$T.forString(\$S, \$S, null)", ClassNames.API_RESPONSE_FIELD, PARAM_TYPE_NAME,
          PARAM_TYPE_NAME)
    } else {
      CodeBlock.of("")
    }
    return fields
        .map { it.responseFieldFactoryStatement() }
        .plus(typeNameFieldStatement)
        .filter { !it.isEmpty }
        .foldIndexed(CodeBlock.builder()) { i, builder, code ->
          builder
              .let { if (i > 0) it.add(",\n") else it }
              .add(code)
        }
        .build()
  }

  private fun Field.responseFieldFactoryStatement(): CodeBlock {
    val scalarTypes = listOf(ClassNames.STRING, TypeName.INT, TypeName.INT.box(), TypeName.LONG, TypeName.LONG.box(),
        TypeName.DOUBLE, TypeName.DOUBLE.box(), TypeName.FLOAT, TypeName.FLOAT.box(), TypeName.BOOLEAN,
        TypeName.BOOLEAN.box())
    val fieldTypeName = fieldSpec().type.withoutAnnotations()
    val factoryMethod = fieldFactoryMethod(fieldTypeName, isOptional())
    val rawFieldType = fieldTypeName.let { if (it.isList()) it.listParamType() else it }
    if (scalarTypes.contains(fieldTypeName)) {
      return scalarResponseFieldFactoryStatement(factoryMethod)
    } else {
      return objectResponseFieldFactoryStatement(factoryMethod, rawFieldType)
    }
  }

  private fun fieldFactoryMethod(type: TypeName, optional: Boolean) = when (type) {
    ClassNames.STRING -> if (optional) "forOptionalString" else "forString"
    TypeName.INT, TypeName.INT.box() -> if (optional) "forOptionalInt" else "forInt"
    TypeName.LONG, TypeName.LONG.box() -> if (optional) "forOptionalLong" else "forLong"
    TypeName.DOUBLE, TypeName.DOUBLE.box() -> if (optional) "forOptionalDouble" else "forDouble"
    TypeName.FLOAT, TypeName.FLOAT.box() -> if (optional) "forOptionalDouble" else "forDouble"
    TypeName.BOOLEAN, TypeName.BOOLEAN.box() -> if (optional) "forOptionalBool" else "forBool"
    else -> {
      if (type.isList()) {
        if (optional) "forOptionalList" else "forList"
      } else {
        if (optional) "forOptionalObject" else "forObject"
      }
    }
  }

  private fun TypeName.isList() = (this is ParameterizedTypeName && rawType == ClassNames.LIST)

  private fun TypeName.listParamType() =
      (this as ParameterizedTypeName)
          .typeArguments
          .first()
          .let { if (it is WildcardTypeName) it.upperBounds.first() else it }

  private fun Field.scalarResponseFieldFactoryStatement(factoryMethod: String): CodeBlock = CodeBlock
      .of("\$T.\$L(\$S, \$S, null)", ClassNames.API_RESPONSE_FIELD, factoryMethod, responseName, fieldName)

  private fun Field.objectResponseFieldFactoryStatement(factoryMethod: String, type: TypeName): CodeBlock = CodeBlock
      .builder()
      .add("\$T.$factoryMethod(\$S, \$S, null, new \$T() {\n", ClassNames.API_RESPONSE_FIELD, responseName, fieldName,
          ParameterizedTypeName.get(ClassNames.API_RESPONSE_FIELD_READER, type))
      .indent()
      .beginControlFlow("@Override public \$T read(\$T \$L)", type, ClassNames.API_RESPONSE_READER, PARAM_READER)
      .addStatement("return new \$T(\$L)", type, PARAM_READER)
      .endControlFlow()
      .unindent()
      .add("})")
      .build()

  companion object {
    private val PARAM_READER = "reader"
    private val PARAM_TYPE_NAME = "__typename"
    private val READER_PARAM_SPEC = ParameterSpec.builder(ClassNames.API_RESPONSE_READER, PARAM_READER).build()
  }
}