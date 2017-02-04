package com.apollographql.android.compiler

import com.apollographql.android.api.graphql.ResponseFieldMapper
import com.apollographql.android.api.graphql.ResponseReader
import com.apollographql.android.compiler.ir.CodeGenerationContext
import com.apollographql.android.compiler.ir.Field
import com.apollographql.android.compiler.ir.InlineFragment
import com.squareup.javapoet.*
import java.io.IOException
import javax.annotation.Nonnull
import javax.lang.model.element.Modifier

class SchemaTypeResponseMapperBuilder(
    typeName: String,
    val fields: List<Field>,
    val fragmentSpreads: List<String>,
    val inlineFragments: List<InlineFragment>,
    val typeOverrideMap: Map<String, String>,
    val context: CodeGenerationContext
) {
  private val typeClassName = ClassName.get("", typeName)
  private val hasFragments = inlineFragments.isNotEmpty() || fragmentSpreads.isNotEmpty()
  private val responseFieldMapperType = ParameterizedTypeName.get(API_RESPONSE_FIELD_MAPPER_TYPE, typeClassName)

  fun build(): TypeSpec {
    val contentValueFields = fields
        .map { it.fieldSpec(context.customTypeMap) }
        .map { FieldSpec.builder(it.type.overrideTypeName(typeOverrideMap), it.name).build() }
        .plus(inlineFragments
            .map { it.fieldSpec() }
            .map { FieldSpec.builder(it.type.overrideTypeName(typeOverrideMap), it.name).build() })
        .let {
          if (fragmentSpreads.isNotEmpty()) {
            it.plus(FRAGMENTS_FIELD)
          } else {
            it
          }
        }
    return TypeSpec.classBuilder("Mapper")
        .addModifiers(Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
        .addSuperinterface(responseFieldMapperType)
        .addMethod(constructor())
        .addType(contentValuesType(contentValueFields))
        .addField(factoryField())
        .addField(fieldArrayField())
        .addMethod(mapMethod(contentValueFields))
        .build()
  }

  private fun factoryField() =
      FieldSpec.builder(Util.FACTORY_INTERFACE_TYPE, FACTORY_VAR, Modifier.FINAL).build()

  private fun constructor(): MethodSpec =
      MethodSpec.constructorBuilder()
          .addModifiers(Modifier.PUBLIC)
          .addParameter(
              ParameterSpec.builder(Util.FACTORY_INTERFACE_TYPE, "factory")
                  .addAnnotation(Nonnull::class.java)
                  .build())
          .addStatement("this.\$L = \$L", FACTORY_VAR, "factory")
          .build()

  private fun contentValuesType(contentValueFields: List<FieldSpec>) =
      TypeSpec.classBuilder(CONTENT_VALUES_TYPE)
          .addModifiers(Modifier.STATIC, Modifier.FINAL)
          .addFields(contentValueFields)
          .build()

  private fun fieldArrayField() =
      FieldSpec.builder(Array<com.apollographql.android.api.graphql.Field>::class.java, FIELDS_VAR)
          .addModifiers(Modifier.FINAL)
          .initializer(CodeBlock.builder()
              .add("{\n")
              .indent()
              .add(fieldFactoryStatements()
                  .filter { !it.isEmpty }
                  .foldIndexed(CodeBlock.builder()) { i, builder, code ->
                    builder.add(if (i > 0) ",\n" else "").add(code)
                  }
                  .build())
              .unindent()
              .add("\n}")
              .build()
          )
          .build()

  private fun mapMethod(contentValueFields: List<FieldSpec>) =
      MethodSpec.methodBuilder("map")
          .addModifiers(Modifier.PUBLIC)
          .addAnnotation(Override::class.java)
          .addParameter(READER_PARAM)
          .addException(IOException::class.java)
          .returns(typeClassName)
          .addCode(mapMethodCode(contentValueFields))
          .build()

  private fun mapMethodCode(contentValueFields: List<FieldSpec>) =
      CodeBlock.builder()
          .addStatement("final \$T \$L = new \$T()", CONTENT_VALUES_TYPE, CONTENT_VALUES_VAR, CONTENT_VALUES_TYPE)
          .add("\$L\$L.read(", READER_PARAM.name, if (hasFragments) ".toBufferedReader()" else "")
          .add("\$L", TypeSpec.anonymousClassBuilder("")
              .superclass(ResponseReader.ValueHandler::class.java)
              .addMethod(valueHandleMethod(contentValueFields))
              .build())
          .add(", \$L);\n", FIELDS_VAR)
          .add("return \$L.\$L().\$L(", FACTORY_VAR, Util.FACTORY_CREATOR_ACCESS_METHOD_NAME,
              Util.CREATOR_CREATE_METHOD_NAME)
          .add(contentValueFields
              .mapIndexed { i, fieldSpec ->
                CodeBlock.of("\$L\$L.\$L", if (i > 0) ", " else "", CONTENT_VALUES_VAR, fieldSpec.name)
              }
              .fold(CodeBlock.builder(), CodeBlock.Builder::add)
              .build())
          .add(");\n")
          .build()

  private fun valueHandleMethod(contentValueFields: List<FieldSpec>) =
      MethodSpec.methodBuilder("handle")
          .addAnnotation(Override::class.java)
          .addModifiers(Modifier.PUBLIC)
          .addParameter(TypeName.INT, FIELD_INDEX_PARAM, Modifier.FINAL)
          .addParameter(TypeName.OBJECT, VALUE_PARAM, Modifier.FINAL)
          .addException(IOException::class.java)
          .addCode(valueHandleSwitchCode(contentValueFields))
          .build()

  private fun valueHandleSwitchCode(contentValueFields: List<FieldSpec>) =
      CodeBlock.builder()
          .beginControlFlow("switch (\$L)", FIELD_INDEX_PARAM)
          .add(contentValueFields
              .mapIndexed { i, field -> fieldValueCaseStatement(field, i) }
              .fold(CodeBlock.builder(), CodeBlock.Builder::add)
              .build())
          .endControlFlow()
          .build()

  private fun fieldValueCaseStatement(fieldSpec: FieldSpec, index: Int): CodeBlock {
    val fieldRawType = fieldSpec.type.withoutAnnotations()
    val setContentValueCode = if (fieldSpec.type.isEnum()) {
      CodeBlock.builder()
          .beginControlFlow("if (\$L != null)", VALUE_PARAM)
          .addStatement("\$L.\$L = \$T.valueOf(\$L)", CONTENT_VALUES_VAR, fieldSpec.name, fieldRawType, VALUE_PARAM)
          .endControlFlow()
          .build()
    } else {
      CodeBlock.of("\$L.\$L = (\$T) \$L;\n", CONTENT_VALUES_VAR, fieldSpec.name, fieldRawType, VALUE_PARAM)
    }
    return CodeBlock.builder()
        .beginControlFlow("case $index:")
        .add(setContentValueCode)
        .addStatement("break")
        .endControlFlow()
        .build()
  }

  private fun fieldFactoryStatements() =
      fields.map { it.responseFieldFactoryStatement() }
          .plus(inlineFragments.map { it.fieldFactoryStatement(it.fieldSpec().type.withoutAnnotations()) })
          .plus(if (fragmentSpreads.isNotEmpty()) fragmentsFieldFactory() else CodeBlock.of(""))

  private fun Field.responseFieldFactoryStatement(): CodeBlock {
    val fieldTypeName = fieldSpec(context.customTypeMap).type.withoutAnnotations()
    if (fieldTypeName.isScalar() || fieldTypeName.isCustomScalarType()) {
      return scalarResponseFieldFactoryStatement(fieldTypeName)
    } else if (fieldTypeName.isList()) {
      return listResponseFieldFactoryStatement(fieldTypeName)
    } else {
      return objectResponseFieldFactoryStatement("forObject", fieldTypeName.overrideTypeName(typeOverrideMap))
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

  private fun TypeName.isList() =
      (this is ParameterizedTypeName && rawType == ClassNames.LIST)

  private fun TypeName.isEnum() =
      ((this is ClassName) && context.typeDeclarations.count { it.kind == "EnumType" && it.name == simpleName() } > 0)

  private fun TypeName.isCustomScalarType() =
      context.customTypeMap.containsValue(toString())

  private fun TypeName.isScalar() = (SCALAR_TYPES.contains(this) || isEnum())

  private fun TypeName.listParamType() =
      (this as ParameterizedTypeName)
          .typeArguments
          .first()
          .let { if (it is WildcardTypeName) it.upperBounds.first() else it }

  private fun Field.scalarResponseFieldFactoryStatement(type: TypeName): CodeBlock {
    if (type.isCustomScalarType()) {
      val customScalarEnum = CustomEnumTypeSpecBuilder.className(context)
      val customScalarEnumConst = normalizeGraphQlType(this.type).toUpperCase()
      return CodeBlock.of("\$T.forCustomType(\$S, \$S, null, \$L, \$T.\$L)", API_RESPONSE_FIELD_TYPE, responseName,
          fieldName, isOptional(), customScalarEnum, customScalarEnumConst)
    } else {
      val factoryMethod = fieldFactoryMethod(type)
      return CodeBlock.of("\$T.\$L(\$S, \$S, null, \$L)", API_RESPONSE_FIELD_TYPE, factoryMethod, responseName,
          fieldName, isOptional())
    }
  }

  private fun Field.objectResponseFieldFactoryStatement(factoryMethod: String, type: TypeName): CodeBlock {
    val typeFactoryMethod = if (type is ParameterizedTypeName) {
      val typeArgument = type.typeArguments[0]
      if (typeArgument is WildcardTypeName) {
        (typeArgument.upperBounds[0] as ClassName).simpleName().decapitalize()
      } else {
        (typeArgument as ClassName).simpleName().decapitalize()
      }
    } else {
      (type as ClassName).simpleName().decapitalize()
    } + "Factory"
    return CodeBlock.builder()
        .add("\$T.$factoryMethod(\$S, \$S, null, \$L, new \$T() {\n", API_RESPONSE_FIELD_TYPE, responseName,
            fieldName, isOptional(), apiResponseFieldObjectReaderTypeName(type))
        .indent()
        .beginControlFlow("@Override public \$T read(final \$T \$L) throws \$T", type,
            ClassNames.API_RESPONSE_READER, READER_PARAM.name, IOException::class.java)
        .add(CodeBlock.of("return new \$T.Mapper(\$L.\$L()).map(\$L);\n", type, FACTORY_VAR, typeFactoryMethod,
            READER_PARAM.name))
        .endControlFlow()
        .unindent()
        .add("})")
        .build()
  }

  private fun Field.listResponseFieldFactoryStatement(type: TypeName): CodeBlock {
    val rawFieldType = type.let { if (it.isList()) it.listParamType() else it }
    return CodeBlock
        .builder()
        .add(
            if (rawFieldType.isCustomScalarType()) {
              readCustomListItemStatement(rawFieldType)
            } else if (rawFieldType.isScalar()) {
              readScalarListItemStatement(rawFieldType)
            } else {
              objectResponseFieldFactoryStatement("forList", rawFieldType.overrideTypeName(typeOverrideMap))
            })
        .build()
  }

  private fun InlineFragment.fieldFactoryStatement(type: TypeName): CodeBlock {
    fun readCodeBlock(): CodeBlock {
      return CodeBlock.builder()
          .beginControlFlow("if (\$L.equals(\$S))", CONDITIONAL_TYPE_VAR, typeCondition)
          .add(CodeBlock.of("return new \$T.Mapper(\$L.\$LFactory()).map(\$L);\n", type,
              FACTORY_VAR, (type as ClassName).simpleName().decapitalize(), READER_PARAM.name))
          .nextControlFlow("else")
          .addStatement("return null")
          .endControlFlow()
          .build()
    }

    fun conditionalFieldReaderType() =
        TypeSpec.anonymousClassBuilder("")
            .superclass(apiConditionalFieldReaderTypeName(type))
            .addMethod(MethodSpec
                .methodBuilder("read")
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(Override::class.java)
                .addException(IOException::class.java)
                .returns(type)
                .addParameter(ParameterSpec.builder(String::class.java, CONDITIONAL_TYPE_VAR).build())
                .addParameter(READER_PARAM)
                .addCode(readCodeBlock())
                .build())
            .build()

    return CodeBlock.of("\$T.forConditionalType(\$S, \$S, \$L)", API_RESPONSE_FIELD_TYPE, "__typename", "__typename",
        conditionalFieldReaderType())
  }

  private fun fragmentsFieldFactory(): CodeBlock {
    fun readCodeBlock(): CodeBlock {
      return CodeBlock.builder()
          .add(CodeBlock.of("return new Fragments.Mapper(\$L.fragmentsFactory(), \$L).map(\$L);\n", FACTORY_VAR,
              CONDITIONAL_TYPE_VAR, READER_PARAM.name))
          .build()
    }

    fun conditionalFieldReaderType() =
        TypeSpec.anonymousClassBuilder("")
            .superclass(apiConditionalFieldReaderTypeName(ClassName.get("", "Fragments")))
            .addMethod(MethodSpec
                .methodBuilder("read")
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(Override::class.java)
                .addException(IOException::class.java)
                .returns(ClassName.get("", "Fragments"))
                .addParameter(ParameterSpec.builder(String::class.java, CONDITIONAL_TYPE_VAR).build())
                .addParameter(READER_PARAM)
                .addCode(readCodeBlock())
                .build())
            .build()

    return CodeBlock.of("\$T.forConditionalType(\$S, \$S, \$L)", API_RESPONSE_FIELD_TYPE, "__typename", "__typename",
        conditionalFieldReaderType())
  }

  private fun apiResponseFieldObjectReaderTypeName(type: TypeName) =
      ParameterizedTypeName.get(API_RESPONSE_FIELD_OBJECT_READER_TYPE, type)

  private fun apiResponseFieldListItemReaderTypeName(type: TypeName) =
      ParameterizedTypeName.get(API_RESPONSE_FIELD_LIST_READER_TYPE, type)

  private fun apiConditionalFieldReaderTypeName(type: TypeName) =
      ParameterizedTypeName.get(API_RESPONSE_FIELD_CONDITIONAL_TYPE_READER_TYPE, type)


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
      CodeBlock.of("return \$T.valueOf(\$L.\$L);\n", type.overrideTypeName(typeOverrideMap), READER_PARAM.name,
          readMethod)
    else
      CodeBlock.of("return \$L.\$L;\n", READER_PARAM.name, readMethod)

    return CodeBlock
        .builder()
        .add("\$T.forList(\$S, \$S, null, \$L, new \$T() {\n", API_RESPONSE_FIELD_TYPE, responseName, fieldName,
            isOptional(), apiResponseFieldListItemReaderTypeName(type.overrideTypeName(typeOverrideMap)))
        .indent()
        .beginControlFlow("@Override public \$T read(final \$T \$L) throws \$T", type.overrideTypeName(typeOverrideMap),
            API_RESPONSE_FIELD_LIST_ITEM_READER_TYPE, READER_PARAM.name, IOException::class.java)
        .add(readStatement)
        .endControlFlow()
        .unindent()
        .add("})")
        .build()
  }

  private fun Field.readCustomListItemStatement(type: TypeName): CodeBlock {
    val customScalarEnum = CustomEnumTypeSpecBuilder.className(context)
    val customScalarEnumConst = normalizeGraphQlType(this.type).toUpperCase()
    return CodeBlock
        .builder()
        .add("\$T.forList(\$S, \$S, null, \$L, new \$T() {\n", API_RESPONSE_FIELD_TYPE, responseName, fieldName,
            isOptional(), apiResponseFieldListItemReaderTypeName(type.overrideTypeName(typeOverrideMap)))
        .indent()
        .beginControlFlow("@Override public \$T read(final \$T \$L) throws \$T", type.overrideTypeName(typeOverrideMap),
            API_RESPONSE_FIELD_LIST_ITEM_READER_TYPE, READER_PARAM.name, IOException::class.java)
        .add(CodeBlock.of("return \$L.readCustomType(\$T.\$L);\n", READER_PARAM.name, customScalarEnum,
            customScalarEnumConst))
        .endControlFlow()
        .unindent()
        .add("})")
        .build()
  }

  companion object {
    private val CONTENT_VALUES_TYPE = ClassName.get("", "__ContentValues")
    private val CONTENT_VALUES_VAR = "contentValues"
    private val FACTORY_VAR = Util.FACTORY_TYPE_NAME.decapitalize()
    private val FIELDS_VAR = "fields"
    private val CONDITIONAL_TYPE_VAR = "conditionalType"
    private val FIELD_INDEX_PARAM = "fieldIndex"
    private val VALUE_PARAM = "value"
    private val READER_PARAM = ParameterSpec.builder(ResponseReader::class.java, "reader").build()
    private val SCALAR_TYPES = listOf(ClassNames.STRING, TypeName.INT, TypeName.INT.box(), TypeName.LONG,
        TypeName.LONG.box(), TypeName.DOUBLE, TypeName.DOUBLE.box(), TypeName.BOOLEAN, TypeName.BOOLEAN.box())
    private val API_RESPONSE_FIELD_TYPE = ClassName.get(com.apollographql.android.api.graphql.Field::class.java)
    private val API_RESPONSE_FIELD_OBJECT_READER_TYPE = ClassName.get(
        com.apollographql.android.api.graphql.Field.ObjectReader::class.java)
    private val API_RESPONSE_FIELD_LIST_READER_TYPE = ClassName.get(
        com.apollographql.android.api.graphql.Field.ListReader::class.java)
    private val API_RESPONSE_FIELD_LIST_ITEM_READER_TYPE = ClassName.get(
        com.apollographql.android.api.graphql.Field.ListItemReader::class.java)
    private val API_RESPONSE_FIELD_MAPPER_TYPE = ClassName.get(ResponseFieldMapper::class.java)
    private val API_RESPONSE_FIELD_CONDITIONAL_TYPE_READER_TYPE = ClassName.get(
        com.apollographql.android.api.graphql.Field.ConditionalTypeReader::class.java)
    private val FRAGMENTS_FIELD = FieldSpec.builder(ClassName.get("", SchemaTypeSpecBuilder.FRAGMENTS_TYPE_NAME),
        SchemaTypeSpecBuilder.FRAGMENTS_TYPE_NAME.decapitalize()).build()

    private fun normalizeGraphQlType(type: String) =
        type.removeSuffix("!").removeSurrounding(prefix = "[", suffix = "]").removeSuffix("!")
  }
}
