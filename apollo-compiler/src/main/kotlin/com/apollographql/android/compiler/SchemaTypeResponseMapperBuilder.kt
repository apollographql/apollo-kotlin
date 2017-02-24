package com.apollographql.android.compiler

import com.apollographql.android.api.graphql.ResponseFieldMapper
import com.apollographql.android.api.graphql.ResponseReader
import com.apollographql.android.compiler.ir.CodeGenerationContext
import com.apollographql.android.compiler.ir.Field
import com.apollographql.android.compiler.ir.InlineFragment
import com.squareup.javapoet.*
import java.io.IOException
import java.util.*
import javax.lang.model.element.Modifier

/**
 * Responsible for any schema type [Mapper] class generation
 *
 * Example of generated class:
 *
 * ```
 *public static final class Mapper implements ResponseFieldMapper<Hero> {
 *  final Field[] fields = {
 *    Field.forString("name", "name", null, false),
 *    Field.forCustomType("birthDate", "birthDate", null, false, CustomType.DATE),
 *    Field.forList("appearanceDates", "appearanceDates", null, false, new Field.ListReader<Date>() {
 *      @Override public Date read(final Field.ListItemReader reader) throws IOException {
 *        return reader.readCustomType(CustomType.DATE);
 *      }
 *    }),
 *    Field.forConditionalType("__typename", "__typename", new Field.ConditionalTypeReader<Fragments>() {
 *      @Override
 *      public Fragments read(String conditionalType, ResponseReader reader) throws IOException {
 *        return new Fragments.Mapper(conditionalType).map(reader);
 *      }
 *    }),
 *    Field.forObject("film", "film", null, true, new Field.ObjectReader<Hero>() {
 *      @Override public Film read(final ResponseReader reader) throws IOException {
 *        return new Film.Mapper().map(reader);
 *      }
 *    })
 *  };
 *
 *  @Override
 *  public Hero map(ResponseReader reader) throws IOException {
 *    final __ContentValues contentValues = new __ContentValues();
 *    reader.read(new ResponseReader.ValueHandler() {
 *      @Override
 *      public void handle(final int fieldIndex, final Object value) throws IOException {
 *        switch (fieldIndex) {
 *          case 0: {
 *            contentValues.name = (String) value;
 *            break;
 *          }
 *          case 1: {
 *            contentValues.birthDate = (Date) value;
 *            break;
 *          }
 *          case 2: {
 *            contentValues.appearanceDates = (List<? extends Date>) value;
 *            break;
 *          }
 *          case 3: {
 *            contentValues.fragments = (Fragments) value;
 *            break;
 *          }
 *          case 4: {
 *            contentValues.film = (Film) value;
 *            break;
 *          }
 *        }
 *      }
 *    }, fields);
 *    return new Hero(contentValues.name, contentValues.birthDate, contentValues.appearanceDates,
 *      fragments, film);
 *  }
 *
 *  static final class __ContentValues {
 *    String name;
 *
 *    Date birthDate;
 *
 *    List<? extends Date> appearanceDates;
 *
 *    Fragments fragments;
 *
 *    Film film;
 *  }
 *}
 *
 * ```
 */
class SchemaTypeResponseMapperBuilder(
    typeName: String,
    val fields: List<Field>,
    val fragmentSpreads: List<String>,
    val inlineFragments: List<InlineFragment>,
    val typeOverrideMap: Map<String, String>,
    val context: CodeGenerationContext
) {
  private val typeClassName = ClassName.get("", typeName)
  private val responseFieldMapperType = ParameterizedTypeName.get(API_RESPONSE_FIELD_MAPPER_TYPE, typeClassName)

  fun build(): TypeSpec {
    val contentValueFields = fields
        .map { it.fieldSpec(customScalarTypeMap = context.customTypeMap) }
        .map { FieldSpec.builder(it.type.overrideTypeName(typeOverrideMap), it.name).build() }
        .plus(inlineFragments
            .map { it.fieldSpec() }
            .map { FieldSpec.builder(it.type.overrideTypeName(typeOverrideMap), it.name).build() })
        .let { if (fragmentSpreads.isNotEmpty()) it.plus(FRAGMENTS_FIELD) else it }

    return TypeSpec.classBuilder(MAPPER_TYPE_NAME)
        .addModifiers(Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
        .addSuperinterface(responseFieldMapperType)
        .addField(fieldArray(fields))
        .addMethod(mapMethod(contentValueFields))
        .build()
  }

  private fun fieldArray(fields: List<Field>) =
      FieldSpec.builder(Array<com.apollographql.android.api.graphql.Field>::class.java, FIELDS_VAR)
          .addModifiers(Modifier.FINAL)
          .initializer(CodeBlock.builder()
              .add("{\n")
              .indent()
              .add(fieldFactoriesCode(fields))
              .unindent()
              .add("\n}")
              .build()
          )
          .build()

  private fun fieldFactoriesCode(fields: List<Field>) =
      fields.map { fieldFactoryCode(it) }
          .plus(inlineFragments.map { inlineFragmentFieldFactoryCode(it) })
          .plus(if (fragmentSpreads.isNotEmpty()) fragmentsFieldFactoryCode() else CodeBlock.of(""))
          .filter { !it.isEmpty }
          .foldIndexed(CodeBlock.builder()) { i, builder, code ->
            builder.add(if (i > 0) ",\n" else "").add(code)
          }
          .build()

  private fun fieldFactoryCode(field: Field): CodeBlock {
    val fieldTypeName = field.fieldSpec(customScalarTypeMap = context.customTypeMap).type.withoutAnnotations()
    if (fieldTypeName.isScalar() || fieldTypeName.isCustomScalarType()) {
      return scalarFieldFactoryCode(field, fieldTypeName)
    } else if (fieldTypeName.isList()) {
      return listFieldFactoryCode(field, fieldTypeName)
    } else {
      return objectFieldFactoryCode(field, "forObject", fieldTypeName.overrideTypeName(typeOverrideMap))
    }
  }

  private fun scalarFieldFactoryCode(field: Field, type: TypeName): CodeBlock {
    if (type.isCustomScalarType()) {
      val customScalarEnum = CustomEnumTypeSpecBuilder.className(context)
      val customScalarEnumConst = normalizeGraphQlType(field.type).toUpperCase(Locale.ENGLISH)
      return CodeBlock.of("\$T.forCustomType(\$S, \$S, \$L, \$L, \$T.\$L)", API_RESPONSE_FIELD_TYPE,
          field.responseName, field.fieldName, field.argumentCodeBlock(), field.isOptional(), customScalarEnum,
          customScalarEnumConst)
    } else {
      val factoryMethod = scalarFieldFactoryMethod(type)
      return CodeBlock.of("\$T.\$L(\$S, \$S, \$L, \$L)", API_RESPONSE_FIELD_TYPE, factoryMethod, field.responseName,
          field.fieldName, field.argumentCodeBlock(), field.isOptional())
    }
  }

  private fun scalarFieldFactoryMethod(type: TypeName) = when (type) {
    ClassNames.STRING -> "forString"
    TypeName.INT, TypeName.INT.box() -> "forInt"
    TypeName.LONG, TypeName.LONG.box() -> "forLong"
    TypeName.DOUBLE, TypeName.DOUBLE.box() -> "forDouble"
    TypeName.BOOLEAN, TypeName.BOOLEAN.box() -> "forBoolean"
    else -> {
      if (type.isEnum()) {
        "forString"
      } else {
        throw RuntimeException("unsupported scalar type $type")
      }
    }
  }

  private fun listFieldFactoryCode(field: Field, type: TypeName): CodeBlock {
    val rawFieldType = type.let { if (it.isList()) it.listParamType() else it }
    return CodeBlock.builder()
        .add(
            if (rawFieldType.isCustomScalarType()) {
              customTypeListFieldFactoryCode(field, rawFieldType)
            } else if (rawFieldType.isScalar()) {
              scalarListFieldFactoryCode(field, rawFieldType)
            } else {
              objectFieldFactoryCode(field, "forList", rawFieldType.overrideTypeName(typeOverrideMap))
            })
        .build()
  }

  private fun customTypeListFieldFactoryCode(field: Field, type: TypeName): CodeBlock {
    val customScalarEnum = CustomEnumTypeSpecBuilder.className(context)
    val customScalarEnumConst = normalizeGraphQlType(field.type).toUpperCase(Locale.ENGLISH)
    return CodeBlock
        .builder()
        .add("\$T.forList(\$S, \$S, \$L, \$L, new \$T() {\n", API_RESPONSE_FIELD_TYPE, field.responseName,
            field.fieldName, field.argumentCodeBlock(), field.isOptional(),
            apiResponseFieldListItemReaderType(type.overrideTypeName(typeOverrideMap)))
        .indent()
        .beginControlFlow("@Override public \$T read(final \$T \$L) throws \$T", type.overrideTypeName(typeOverrideMap),
            API_RESPONSE_FIELD_LIST_ITEM_READER_TYPE, READER_VAR, IOException::class.java)
        .add(CodeBlock.of("return \$L.readCustomType(\$T.\$L);\n", READER_VAR, customScalarEnum,
            customScalarEnumConst))
        .endControlFlow()
        .unindent()
        .add("})")
        .build()
  }

  private fun apiResponseFieldListItemReaderType(type: TypeName) =
      ParameterizedTypeName.get(API_RESPONSE_FIELD_LIST_READER_TYPE, type)

  private fun scalarListFieldFactoryCode(field: Field, type: TypeName): CodeBlock {
    val readMethod = when (type) {
      ClassNames.STRING -> "readString()"
      TypeName.INT, TypeName.INT.box() -> "readInt()"
      TypeName.LONG, TypeName.LONG.box() -> "readLong()"
      TypeName.DOUBLE, TypeName.DOUBLE.box() -> "readDouble()"
      TypeName.BOOLEAN, TypeName.BOOLEAN.box() -> "readBoolean()"
      else -> "readString()"
    }

    val readStatement = if (type.isEnum())
      CodeBlock.of("return \$T.valueOf(\$L.\$L);\n", type.overrideTypeName(typeOverrideMap), READER_VAR, readMethod)
    else
      CodeBlock.of("return \$L.\$L;\n", READER_VAR, readMethod)

    return CodeBlock
        .builder()
        .add("\$T.forList(\$S, \$S, \$L, \$L, new \$T() {\n", API_RESPONSE_FIELD_TYPE, field.responseName,
            field.fieldName, field.argumentCodeBlock(), field.isOptional(),
            apiResponseFieldListItemReaderType(type.overrideTypeName(typeOverrideMap)))
        .indent()
        .beginControlFlow("@Override public \$T read(final \$T \$L) throws \$T", type.overrideTypeName(typeOverrideMap),
            API_RESPONSE_FIELD_LIST_ITEM_READER_TYPE, READER_VAR, IOException::class.java)
        .add(readStatement)
        .endControlFlow()
        .unindent()
        .add("})")
        .build()
  }

  private fun objectFieldFactoryCode(field: Field, factoryMethod: String, type: TypeName): CodeBlock {
    return CodeBlock.builder()
        .add("\$T.$factoryMethod(\$S, \$S, \$L, \$L, new \$T() {\n", API_RESPONSE_FIELD_TYPE, field.responseName,
            field.fieldName, field.argumentCodeBlock(), field.isOptional(), apiResponseFieldObjectReaderTypeName(type))
        .indent()
        .beginControlFlow("@Override public \$T read(final \$T \$L) throws \$T", type,
            ClassNames.API_RESPONSE_READER, READER_VAR, IOException::class.java)
        .add(CodeBlock.of("return new \$T.Mapper().map(\$L);\n", type, READER_VAR))
        .endControlFlow()
        .unindent()
        .add("})")
        .build()
  }

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
          .add(contentValueFields
              .mapIndexed { i, fieldSpec -> readFieldValueCode(fieldSpec, i) }
              .fold(CodeBlock.builder(), CodeBlock.Builder::add)
              .build())
          .add("return new \$T(", typeClassName)
          .add(contentValueFields
              .mapIndexed { i, fieldSpec -> CodeBlock.of("\$L\$L", if (i > 0) ", " else "", fieldSpec.name) }
              .fold(CodeBlock.builder(), CodeBlock.Builder::add)
              .build())
          .add(");\n")
          .build()


  private fun readFieldValueCode(fieldSpec: FieldSpec, index: Int): CodeBlock {
    val fieldRawType = fieldSpec.type.withoutAnnotations()
    return if (fieldSpec.type.isEnum()) {
      CodeBlock.builder()
          .addStatement("final \$T \$LStr = \$L.read(\$L[\$L])", ClassNames.STRING, fieldSpec.name, READER_VAR,
              FIELDS_VAR, index)
          .addStatement("final \$T \$L", fieldRawType, fieldSpec.name)
          .beginControlFlow("if (\$LStr != null)", fieldSpec.name)
          .addStatement("\$L = \$T.valueOf(\$LStr)", fieldSpec.name, fieldRawType, fieldSpec.name)
          .nextControlFlow("else")
          .addStatement("\$L = null", fieldSpec.name)
          .endControlFlow()
          .build()
    } else {
      CodeBlock.of("final \$T \$L = \$L.read(\$L[\$L]);\n", fieldRawType, fieldSpec.name, READER_VAR, FIELDS_VAR, index)
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

  private fun inlineFragmentFieldFactoryCode(fragment: InlineFragment): CodeBlock {
    val type = fragment.fieldSpec().type.withoutAnnotations()
    fun readCodeBlock(): CodeBlock {
      return CodeBlock.builder()
          .beginControlFlow("if (\$L.equals(\$S))", CONDITIONAL_TYPE_VAR, fragment.typeCondition)
          .add(CodeBlock.of("return new \$T.Mapper().map(\$L);\n", type, READER_PARAM.name))
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

  private fun fragmentsFieldFactoryCode(): CodeBlock {
    fun readCodeBlock(): CodeBlock {
      return CodeBlock.builder()
          .add(CodeBlock.of("return new Fragments.Mapper(\$L).map(\$L);\n", CONDITIONAL_TYPE_VAR, READER_PARAM.name))
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

  private fun apiConditionalFieldReaderTypeName(type: TypeName) =
      ParameterizedTypeName.get(API_RESPONSE_FIELD_CONDITIONAL_TYPE_READER_TYPE, type)

  companion object {
    val MAPPER_TYPE_NAME: String = "Mapper"
    private val FRAGMENTS_FIELD = FieldSpec.builder(ClassName.get("", SchemaTypeSpecBuilder.FRAGMENTS_TYPE_NAME),
        SchemaTypeSpecBuilder.FRAGMENTS_TYPE_NAME.decapitalize()).build()
    private val FIELDS_VAR = "fields"
    private val CONDITIONAL_TYPE_VAR = "conditionalType"
    private val VALUE_PARAM = "value"
    private val READER_VAR = "reader"
    private val READER_PARAM = ParameterSpec.builder(ResponseReader::class.java, READER_VAR).build()
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

    private fun normalizeGraphQlType(type: String) =
        type.removeSuffix("!").removeSurrounding(prefix = "[", suffix = "]").removeSuffix("!")
  }
}
