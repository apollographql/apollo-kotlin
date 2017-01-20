package com.apollostack.compiler

import com.apollostack.api.graphql.ResponseFieldMapper
import com.apollostack.api.graphql.ResponseReader
import com.apollostack.compiler.ir.Field
import com.apollostack.compiler.ir.InlineFragment
import com.apollostack.compiler.ir.TypeDeclaration
import com.squareup.javapoet.*
import java.io.IOException
import javax.lang.model.element.Modifier

class ResponseFieldMapperBuilder(
    typeName: String,
    val fields: List<Field>,
    val fragmentSpreads: List<String>,
    val inlineFragments: List<InlineFragment>,
    val typeOverrideMap: Map<String, String>,
    val typeDeclarations: List<TypeDeclaration>
) {
  private val typeClassName = ClassName.get("", typeName)
  private val hasFragments = inlineFragments.isNotEmpty() || fragmentSpreads.isNotEmpty()
  private val responseFieldMapperType = ParameterizedTypeName.get(API_RESPONSE_FIELD_MAPPER, typeClassName)

  fun build(): FieldSpec = FieldSpec
      .builder(responseFieldMapperType, "MAPPER")
      .addModifiers(Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL)
      .initializer("\$L", mapperType())
      .build()

  private fun mapperType() = TypeSpec
      .anonymousClassBuilder("")
      .superclass(responseFieldMapperType)
      .addField(fieldArrayField())
      .addMethod(mapMethod())
      .build()

  private fun fieldArrayField() = FieldSpec
      .builder(Array<com.apollostack.api.graphql.Field>::class.java, "FIELDS", Modifier.PRIVATE, Modifier.FINAL)
      .initializer(CodeBlock
          .builder()
          .add("{\n")
          .indent()
          .add(responseFieldFactoryStatements())
          .unindent()
          .add("\n}")
          .build()
      )
      .build()

  private fun mapMethod() = MethodSpec
      .methodBuilder("map")
      .addModifiers(Modifier.PUBLIC)
      .addAnnotation(Override::class.java)
      .addParameter(PARAM_SPEC_READER)
      .addParameter(ParameterSpec.builder(typeClassName, PARAM_INSTANCE, Modifier.FINAL).build())
      .addException(IOException::class.java)
      .addCode(mapMethodCode())
      .build()

  private fun mapMethodCode() = CodeBlock
      .builder()
      .add("\$L.read(", PARAM_READER)
      .add("\$L", valueHandlerType())
      .add(", FIELDS);\n")
      .build()

  private fun valueHandlerType() = TypeSpec
      .anonymousClassBuilder("")
      .superclass(ResponseReader.ValueHandler::class.java)
      .addMethod(valueHandlerHandleMethod())
      .build()

  private fun valueHandlerHandleMethod() = MethodSpec
      .methodBuilder("handle")
      .addAnnotation(Override::class.java)
      .addModifiers(Modifier.PUBLIC)
      .addParameter(TypeName.INT, PARAM_FIELD_INDEX, Modifier.FINAL)
      .addParameter(TypeName.OBJECT, PARAM_VALUE, Modifier.FINAL)
      .addException(IOException::class.java)
      .addCode(CodeBlock
          .builder()
          .beginControlFlow("switch (\$L)", PARAM_FIELD_INDEX)
          .add(fields
              .mapIndexed { i, field -> fieldValueCaseStatement(field, i) }
              .fold(CodeBlock.builder(), CodeBlock.Builder::add)
              .build())
          .add(valueHandlerFragmentsCaseStatement(fields.size))
          .endControlFlow()
          .build())
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
                  .addStatement("\$L.\$L = \$T.valueOf(\$L)", PARAM_INSTANCE, fieldSpec.name, fieldRawType, PARAM_VALUE)
                  .endControlFlow()
                  .build()
            } else {
              CodeBlock.of("\$L.\$L = (\$T) \$L;\n", PARAM_INSTANCE, fieldSpec.name, fieldRawType, PARAM_VALUE)
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
          .addStatement("\$T \$L = (\$T) \$L", String::class.java, PARAM_TYPE_NAME, String::class.java, PARAM_VALUE)
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
        .addStatement("\$L.\$L = new \$T(\$L)", PARAM_INSTANCE, fieldSpec.name, fieldRawType, PARAM_READER)
        .endControlFlow()
        .build()
  }

  private fun fragmentsInitStatement(): CodeBlock {
    if (fragmentSpreads.isNotEmpty()) {
      return CodeBlock.builder()
          .addStatement("\$L.\$L = new \$L(\$L, \$L)", PARAM_INSTANCE,
              SchemaTypeSpecBuilder.FRAGMENTS_INTERFACE_NAME.decapitalize(),
              SchemaTypeSpecBuilder.FRAGMENTS_INTERFACE_NAME, PARAM_READER, PARAM_TYPE_NAME)
          .build()
    } else {
      return CodeBlock.of("")
    }
  }

  private fun responseFieldFactoryStatements(): CodeBlock {
    val typeNameFieldStatement = if (hasFragments) {
      CodeBlock.of("\$T.forString(\$S, \$S, null, false)", com.apollostack.api.graphql.Field::class.java, "__typename",
          "__typename")
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
      return objectResponseFieldFactoryStatement("forObject", fieldTypeName)
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
    return CodeBlock.of("\$T.\$L(\$S, \$S, null, \$L)", API_RESPONSE_FIELD, factoryMethod, responseName,
        fieldName, isOptional())
  }

  private fun Field.objectResponseFieldFactoryStatement(factoryMethod: String, type: TypeName) = CodeBlock
      .builder()
      .add("\$T.$factoryMethod(\$S, \$S, null, \$L, new \$T() {\n", API_RESPONSE_FIELD, responseName,
          fieldName, isOptional(), apiResponseFieldReaderTypeName(type.overrideTypeName(typeOverrideMap)))
      .indent()
      .beginControlFlow("@Override public \$T read(final \$T \$L) throws \$T", type.overrideTypeName(typeOverrideMap),
          ClassNames.API_RESPONSE_READER, PARAM_READER, IOException::class.java)
      .add(CodeBlock.of("return new \$T(\$L);\n", type.overrideTypeName(typeOverrideMap), PARAM_READER))
      .endControlFlow()
      .unindent()
      .add("})")
      .build()

  private fun Field.listResponseFieldFactoryStatement(type: TypeName): CodeBlock {
    val rawFieldType = type.let { if (it.isList()) it.listParamType() else it }
    return CodeBlock
        .builder()
        .add(
            if (rawFieldType.isScalar()) {
              readScalarListItemStatement(rawFieldType)
            } else {
              objectResponseFieldFactoryStatement("forList", rawFieldType)
            })
        .build()
  }

  private fun apiResponseFieldReaderTypeName(type: TypeName) =
      ParameterizedTypeName.get(API_RESPONSE_FIELD_OBJECT_READER, type.overrideTypeName(typeOverrideMap))

  private fun apiResponseFieldListItemReaderTypeName(type: TypeName) =
      ParameterizedTypeName.get(API_RESPONSE_FIELD_LIST_READER, type.overrideTypeName(typeOverrideMap))

  private fun Field.readScalarListItemStatement(type: TypeName): CodeBlock {
    val readMethod = when (type) {
      ClassNames.STRING -> "readString()"
      TypeName.INT, TypeName.INT.box() -> "readInt()"
      TypeName.LONG, TypeName.LONG.box() -> "readLong()"
      TypeName.DOUBLE, TypeName.DOUBLE.box() -> "readDouble()"
      TypeName.BOOLEAN, TypeName.BOOLEAN.box() -> "readBoolean()"
      else -> "readString()"
    }

    val readStatement = if (type.isEnum())
      CodeBlock.of("return \$T.valueOf(\$L.\$L);\n", type.overrideTypeName(typeOverrideMap), PARAM_READER, readMethod)
    else
      CodeBlock.of("return \$L.\$L;\n", PARAM_READER, readMethod);

    return CodeBlock
        .builder()
        .add("\$T.forList(\$S, \$S, null, \$L, new \$T() {\n", API_RESPONSE_FIELD, responseName, fieldName,
            isOptional(), apiResponseFieldListItemReaderTypeName(type.overrideTypeName(typeOverrideMap)))
        .indent()
        .beginControlFlow("@Override public \$T read(final \$T \$L) throws \$T", type.overrideTypeName(typeOverrideMap),
            API_RESPONSE_FIELD_LIST_ITEM_READER, PARAM_READER, IOException::class.java)
        .add(readStatement)
        .endControlFlow()
        .unindent()
        .add("})")
        .build()
  }

  companion object {
    private val PARAM_READER = "reader"
    private val PARAM_INSTANCE = "instance"
    private val PARAM_TYPE_NAME = "typename"
    private val PARAM_FIELD_INDEX = "fieldIndex"
    private val PARAM_VALUE = "value"
    private val PARAM_SPEC_READER = ParameterSpec.builder(ResponseReader::class.java, PARAM_READER,
        Modifier.FINAL).build()
    private val SCALAR_TYPES = listOf(ClassNames.STRING, TypeName.INT, TypeName.INT.box(), TypeName.LONG,
        TypeName.LONG.box(), TypeName.DOUBLE, TypeName.DOUBLE.box(), TypeName.BOOLEAN, TypeName.BOOLEAN.box())
    private val API_RESPONSE_FIELD = ClassName.get(com.apollostack.api.graphql.Field::class.java)
    private val API_RESPONSE_FIELD_OBJECT_READER = ClassName.get(
        com.apollostack.api.graphql.Field.ObjectReader::class.java)
    private val API_RESPONSE_FIELD_LIST_READER = ClassName.get(com.apollostack.api.graphql.Field.ListReader::class.java)
    private val API_RESPONSE_FIELD_LIST_ITEM_READER = ClassName.get(
        com.apollostack.api.graphql.Field.ListItemReader::class.java)
    private val API_RESPONSE_FIELD_MAPPER = ClassName.get(ResponseFieldMapper::class.java)
  }
}
