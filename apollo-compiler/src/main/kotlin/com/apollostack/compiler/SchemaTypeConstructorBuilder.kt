package com.apollostack.compiler

import com.apollostack.compiler.ir.Field
import com.apollostack.compiler.ir.InlineFragment
import com.apollostack.compiler.ir.TypeDeclaration
import com.squareup.javapoet.*
import javax.lang.model.element.Modifier

class SchemaTypeConstructorBuilder(
    val fields: List<Field>,
    val fragmentSpreads: List<String>,
    val inlineFragments: List<InlineFragment>,
    val typeOverrideMap: Map<String, String>,
    val typeDeclarations: List<TypeDeclaration>
) {
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
            CodeBlock.builder()
                .addStatement("final \$T \$L = \$L.toBufferedReader()", ClassNames.API_RESPONSE_READER,
                    PARAM_BUFFERED_READER, PARAM_READER)
                .add("\$L.read(\n", PARAM_BUFFERED_READER)
                .build()
          } else {
            CodeBlock.of("\$L.read(\n", PARAM_READER)
          })
      .build()

  private fun valueHandlerStatement(): CodeBlock = CodeBlock
      .builder()
      .add("new \$T() {\n", ClassNames.API_RESPONSE_VALUE_HANDLER)
      .indent()
      .beginControlFlow("@Override public void handle(int \$L, Object \$L) throws \$T", PARAM_FIELD_INDEX, PARAM_VALUE,
          ClassNames.IO_EXCEPTION)
      .add(valueHandlerSwitchStatement())
      .endControlFlow()
      .unindent()
      .add("}")
      .build()

  private fun valueHandlerSwitchStatement(): CodeBlock = CodeBlock
      .builder()
      .beginControlFlow("switch (\$L)", PARAM_FIELD_INDEX)
      .add(fields
          .mapIndexed { i, field -> fieldValueCaseStatement(field, i) }
          .fold(CodeBlock.builder(), CodeBlock.Builder::add)
          .build())
      .add(valueHandlerFragmentsCaseStatement(fields.size))
      .endControlFlow()
      .build()

  private fun fieldValueCaseStatement(field: Field, index: Int): CodeBlock {
    val fieldSpec = field.fieldSpec()
    val fieldRawType = fieldSpec.type.withoutAnnotations().overrideTypeName(typeOverrideMap)
    return CodeBlock.builder()
        .beginControlFlow("case $index:")
        .add(
            if (fieldRawType.isEnum()) {
              CodeBlock
                  .builder()
                  .beginControlFlow("if (\$L != null)", PARAM_VALUE)
                  .addStatement("\$L = \$T.valueOf(\$L)", fieldSpec.name, fieldRawType, PARAM_VALUE)
                  .endControlFlow()
                  .build()
            } else {
              CodeBlock.of("\$L = (\$T) \$L;\n", fieldSpec.name, fieldRawType, PARAM_VALUE)
            })
        .addStatement("break")
        .endControlFlow()
        .build()
  }

  private fun valueHandlerFragmentsCaseStatement(index: Int): CodeBlock {
    if (hasFragments) {
      return CodeBlock
          .builder()
          .beginControlFlow("case $index:")
          .addStatement("\$T \$L = (\$T) \$L", ClassNames.STRING, PARAM_TYPE_NAME, ClassNames.STRING, PARAM_VALUE)
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
        .addStatement("\$L = new \$T(\$L)", fieldSpec.name, fieldRawType, PARAM_BUFFERED_READER)
        .endControlFlow()
        .build()
  }

  private fun fragmentsInitStatement(): CodeBlock {
    if (fragmentSpreads.isNotEmpty()) {
      return CodeBlock.builder()
          .addStatement("\$L = new \$L(\$L, \$L)", SchemaTypeSpecBuilder.FRAGMENTS_INTERFACE_NAME.decapitalize(),
              SchemaTypeSpecBuilder.FRAGMENTS_INTERFACE_NAME, PARAM_BUFFERED_READER, PARAM_TYPE_NAME)
          .build()
    } else {
      return CodeBlock.of("")
    }
  }

  private fun responseFieldFactoryStatements(): CodeBlock {
    val typeNameFieldStatement = if (hasFragments) {
      CodeBlock.of("\$T.forString(\$S, \$S, null, false)", ClassNames.API_RESPONSE_FIELD, "__typename", "__typename")
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
    val fieldTypeName = fieldSpec().type.withoutAnnotations()
    if (fieldTypeName.isScalar()) {
      return scalarResponseFieldFactoryStatement(fieldTypeName)
    } else if (fieldTypeName.isList()) {
      return listResponseFieldFactoryStatement(fieldTypeName)
    } else {
      return objectResponseFieldFactoryStatement(fieldTypeName)
    }
  }

  private fun fieldFactoryMethod(type: TypeName) = when (type) {
    ClassNames.STRING -> "forString"
    TypeName.INT, TypeName.INT.box() -> "forInt"
    TypeName.LONG, TypeName.LONG.box() -> "forLong"
    TypeName.DOUBLE, TypeName.DOUBLE.box() -> "forDouble"
    TypeName.BOOLEAN, TypeName.BOOLEAN.box() -> "forBoolean"
    else -> {
      if (type.isEnum()) {
        "forString"
      } else if (type.isList()) {
        "forList"
      } else {
        "forObject"
      }
    }
  }

  private fun TypeName.isList() = (this is ParameterizedTypeName && rawType == ClassNames.LIST)

  private fun TypeName.isEnum() = ((this is ClassName) && typeDeclarations.count { it.kind == "EnumType" && it.name == simpleName() } > 0)

  private fun TypeName.isScalar() = (SCALAR_TYPES.contains(this) || isEnum())

  private fun TypeName.listParamType() =
      (this as ParameterizedTypeName)
          .typeArguments
          .first()
          .let { if (it is WildcardTypeName) it.upperBounds.first() else it }

  private fun Field.scalarResponseFieldFactoryStatement(type: TypeName): CodeBlock {
    val factoryMethod = fieldFactoryMethod(type)
    return CodeBlock.of("\$T.\$L(\$S, \$S, null, \$L)", ClassNames.API_RESPONSE_FIELD, factoryMethod, responseName,
        fieldName, isOptional())
  }

  private fun Field.objectResponseFieldFactoryStatement(type: TypeName): CodeBlock = CodeBlock
      .builder()
      .add("\$T.forObject(\$S, \$S, null, \$L, new \$T() {\n", ClassNames.API_RESPONSE_FIELD, responseName,
          fieldName, isOptional(), apiResponseFieldReaderTypeName(type.overrideTypeName(typeOverrideMap)))
      .indent()
      .beginControlFlow("@Override public \$T read(final \$T \$L) throws \$T", type.overrideTypeName(typeOverrideMap),
          ClassNames.API_RESPONSE_READER, PARAM_READER, ClassNames.IO_EXCEPTION)
      .add(CodeBlock.of("return new \$T(\$L);\n", type.overrideTypeName(typeOverrideMap), PARAM_READER))
      .endControlFlow()
      .unindent()
      .add("})")
      .build()

  private fun Field.listResponseFieldFactoryStatement(type: TypeName): CodeBlock {
    val rawFieldType = type.let { if (it.isList()) it.listParamType() else it }
    return CodeBlock
        .builder()
        .add("\$T.forList(\$S, \$S, null, \$L, new \$T() {\n", ClassNames.API_RESPONSE_FIELD, responseName, fieldName,
            isOptional(), apiResponseFieldListItemReaderTypeName(rawFieldType.overrideTypeName(typeOverrideMap)))
        .indent()
        .beginControlFlow("@Override public \$T read(final \$T \$L) throws \$T",
            rawFieldType.overrideTypeName(typeOverrideMap), ClassNames.API_RESPONSE_FIELD_LIST_ITEM_READER,
            PARAM_READER,
            ClassNames.IO_EXCEPTION)
        .add(if (rawFieldType.isScalar()) readScalarListItemStatement(rawFieldType) else readObjectListItemStatement(
            rawFieldType))
        .endControlFlow()
        .unindent()
        .add("})")
        .build()
  }

  private fun apiResponseFieldReaderTypeName(type: TypeName) =
      ParameterizedTypeName.get(ClassNames.API_RESPONSE_FIELD_READER, type.overrideTypeName(typeOverrideMap))

  private fun apiResponseFieldListItemReaderTypeName(type: TypeName) =
      ParameterizedTypeName.get(ClassNames.API_RESPONSE_FIELD_LIST_READER, type.overrideTypeName(typeOverrideMap))

  private fun readScalarListItemStatement(type: TypeName): CodeBlock {
    val readMethod = when (type) {
      ClassNames.STRING -> "readString()"
      TypeName.INT, TypeName.INT.box() -> "readInt()"
      TypeName.LONG, TypeName.LONG.box() -> "readLong()"
      TypeName.DOUBLE, TypeName.DOUBLE.box() -> "readDouble()"
      TypeName.BOOLEAN, TypeName.BOOLEAN.box() -> "readBoolean()"
      else -> "readString()"
    }
    if (type.isEnum()) {
      return CodeBlock.of("return \$T.valueOf(\$L.\$L);\n", type.overrideTypeName(typeOverrideMap), PARAM_READER,
          readMethod)
    } else {
      return CodeBlock.of("return \$L.\$L;\n", PARAM_READER, readMethod);
    }
  }

  private fun readObjectListItemStatement(type: TypeName): CodeBlock = CodeBlock
      .builder()
      .add("return \$L.readObject(new \$T() {\n", PARAM_READER,
          apiResponseFieldReaderTypeName(type.overrideTypeName(typeOverrideMap)))
      .indent()
      .beginControlFlow("@Override public \$T read(final \$T \$L) throws \$T", type.overrideTypeName(typeOverrideMap),
          ClassNames.API_RESPONSE_READER, PARAM_READER, ClassNames.IO_EXCEPTION)
      .add(CodeBlock.of("return new \$T(\$L);\n", type.overrideTypeName(typeOverrideMap), PARAM_READER))
      .endControlFlow()
      .unindent()
      .add("});\n")
      .build()

  companion object {
    private val PARAM_READER = "reader"
    private val PARAM_BUFFERED_READER = "bufferedReader"
    private val PARAM_TYPE_NAME = "typename__"
    private val PARAM_FIELD_INDEX = "fieldIndex__"
    private val PARAM_VALUE = "value__"
    private val READER_PARAM_SPEC = ParameterSpec.builder(ClassNames.API_RESPONSE_READER, PARAM_READER).build()
    private val SCALAR_TYPES = listOf(ClassNames.STRING, TypeName.INT, TypeName.INT.box(), TypeName.LONG,
        TypeName.LONG.box(), TypeName.DOUBLE, TypeName.DOUBLE.box(), TypeName.BOOLEAN, TypeName.BOOLEAN.box())
  }
}