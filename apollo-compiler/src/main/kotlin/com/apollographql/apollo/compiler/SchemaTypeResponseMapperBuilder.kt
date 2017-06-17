package com.apollographql.apollo.compiler

import com.apollographql.apollo.api.ResponseFieldMapper
import com.apollographql.apollo.api.ResponseReader
import com.apollographql.apollo.compiler.ir.CodeGenerationContext
import com.apollographql.apollo.compiler.ir.Field
import com.apollographql.apollo.compiler.ir.InlineFragment
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
 *public static final class Mapper implements ResponseFieldMapper<Data> {
 *     final Hero.Mapper heroFieldMapper = new Hero.Mapper();
 *
 *     final Field[] fields = {
 *       Field.forObject("hero", "hero", null, true, new Field.ObjectReader<Hero>() {
 *         @Override public Hero read(final ResponseReader reader) throws IOException {
 *           return heroFieldMapper.map(reader);
 *         }
 *       })
 *     };
 *
 *     @Override
 *     public Data map(ResponseReader reader) throws IOException {
 *       final Hero hero = reader.read(fields[0]);
 *       return new Data(hero);
 *     }
 *   }
 * }
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
  private val apiFieldFactoryMethods = mapOf(
      com.apollographql.apollo.api.Field.Type.STRING to "forString",
      com.apollographql.apollo.api.Field.Type.INT to "forInt",
      com.apollographql.apollo.api.Field.Type.LONG to "forLong",
      com.apollographql.apollo.api.Field.Type.DOUBLE to "forDouble",
      com.apollographql.apollo.api.Field.Type.BOOLEAN to "forBoolean",
      com.apollographql.apollo.api.Field.Type.ENUM to "forString",
      com.apollographql.apollo.api.Field.Type.OBJECT to "forObject",
      com.apollographql.apollo.api.Field.Type.SCALAR_LIST to "forScalarList",
      com.apollographql.apollo.api.Field.Type.CUSTOM_LIST to "forCustomList",
      com.apollographql.apollo.api.Field.Type.OBJECT_LIST to "forObjectList",
      com.apollographql.apollo.api.Field.Type.CUSTOM to "forCustomType",
      com.apollographql.apollo.api.Field.Type.FRAGMENT to "forFragment",
      com.apollographql.apollo.api.Field.Type.INLINE_FRAGMENT to "forInlineFragment"
  )
  private val apiFieldValueReadMethods = mapOf(
      com.apollographql.apollo.api.Field.Type.STRING to "readString",
      com.apollographql.apollo.api.Field.Type.INT to "readInt",
      com.apollographql.apollo.api.Field.Type.LONG to "readLong",
      com.apollographql.apollo.api.Field.Type.DOUBLE to "readDouble",
      com.apollographql.apollo.api.Field.Type.BOOLEAN to "readBoolean",
      com.apollographql.apollo.api.Field.Type.ENUM to "readString",
      com.apollographql.apollo.api.Field.Type.OBJECT to "readObject",
      com.apollographql.apollo.api.Field.Type.SCALAR_LIST to "readList",
      com.apollographql.apollo.api.Field.Type.CUSTOM_LIST to "readList",
      com.apollographql.apollo.api.Field.Type.OBJECT_LIST to "readList",
      com.apollographql.apollo.api.Field.Type.CUSTOM to "readCustomType",
      com.apollographql.apollo.api.Field.Type.FRAGMENT to "readConditional",
      com.apollographql.apollo.api.Field.Type.INLINE_FRAGMENT to "readConditional"
  )
  private val scalarListValueReadMethods = mapOf(
      TypeName.INT to "readInt()",
      TypeName.INT.box() to "readInt()",
      TypeName.LONG to "readLong()",
      TypeName.LONG.box() to "readLong()",
      TypeName.DOUBLE to "readDouble()",
      TypeName.DOUBLE.box() to "readDouble()",
      TypeName.BOOLEAN to "readBoolean()",
      TypeName.BOOLEAN.box() to "readBoolean()"
  )
  private val mapperFields = fields.map { schemaField ->
    val fieldSpec = schemaField.fieldSpec(context).let {
      FieldSpec.builder(
          it.type
              .overrideTypeName(typeOverrideMap)
              .unwrapOptionalType()
              .withoutAnnotations(),
          it.name
      ).build()
    }
    MapperField(
        schemaField = schemaField,
        fieldSpec = fieldSpec,
        apiFieldType = if (fieldSpec.type.isEnum())
          com.apollographql.apollo.api.Field.Type.ENUM
        else
          apiFieldType(schemaField, fieldSpec)
    )
  }
  private val mapperInlineFragmentFields = inlineFragments.map { inlineFragment ->
    val fieldSpec = inlineFragment.fieldSpec(context).let {
      FieldSpec.builder(
          it.type
              .overrideTypeName(typeOverrideMap)
              .unwrapOptionalType()
              .withoutAnnotations(),
          it.name
      ).build()
    }
    MapperField(
        schemaField = Field.TYPE_NAME_FIELD,
        fieldSpec = fieldSpec,
        apiFieldType = com.apollographql.apollo.api.Field.Type.INLINE_FRAGMENT,
        typeConditions = if (inlineFragment.possibleTypes != null && !inlineFragment.possibleTypes.isEmpty())
          inlineFragment.possibleTypes
        else
          listOf(inlineFragment.typeCondition)
    )
  }
  private val mapperFragmentFields = if (fragmentSpreads.isNotEmpty())
    listOf(MapperField(
        schemaField = Field.TYPE_NAME_FIELD,
        fieldSpec = FRAGMENTS_FIELD,
        apiFieldType = com.apollographql.apollo.api.Field.Type.FRAGMENT,
        typeConditions = context.ir.fragments
            .filter { it.fragmentName in fragmentSpreads }
            .flatMap { it.possibleTypes }
    ))
  else
    emptyList()

  fun build(): TypeSpec {
    val mapperFields = mapperFields + mapperInlineFragmentFields + mapperFragmentFields
    return TypeSpec.classBuilder(Util.MAPPER_TYPE_NAME)
        .addModifiers(Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
        .addSuperinterface(responseFieldMapperType)
        .addFields(mapperFields())
        .addField(fieldArray(mapperFields))
        .addMethod(mapMethod(mapperFields))
        .build()
  }

  private fun fieldArray(fields: List<MapperField>): FieldSpec {
    fun customTypeFactoryCode(schemaField: Field, factoryMethod: String): CodeBlock {
      val customScalarEnum = CustomEnumTypeSpecBuilder.className(context)
      val customScalarEnumConst = normalizeGraphQlType(schemaField.type).toUpperCase(Locale.ENGLISH)
      return CodeBlock.of("\$T.\$L(\$S, \$S, \$L, \$L, \$T.\$L)", API_RESPONSE_FIELD_TYPE, factoryMethod,
          schemaField.responseName, schemaField.fieldName, schemaField.argumentCodeBlock(),
          schemaField.isOptional(), customScalarEnum, customScalarEnumConst)
    }

    fun fragmentFactoryCode(schemaField: Field, factoryMethod: String, typeConditions: List<String>): CodeBlock {
      val typeConditionListCode = typeConditions
          .foldIndexed(CodeBlock.builder().add("\$T.asList(", Arrays::class.java)) { i, builder, typeCondition ->
            builder.add(if (i > 0) ",\n" else "").add("\$S", typeCondition)
          }
          .add(")")
          .build()
      return CodeBlock.of("\$T.\$L(\$S, \$S, \$L)", API_RESPONSE_FIELD_TYPE, factoryMethod, schemaField.responseName,
          schemaField.fieldName, typeConditionListCode)
    }

    fun genericFactoryCode(schemaField: Field, factoryMethod: String): CodeBlock {
      return CodeBlock.of("\$T.\$L(\$S, \$S, \$L, \$L)", API_RESPONSE_FIELD_TYPE, factoryMethod,
          schemaField.responseName, schemaField.fieldName, schemaField.argumentCodeBlock(), schemaField.isOptional())
    }

    fun MapperField.factoryCode(): CodeBlock {
      val factoryMethod = apiFieldFactoryMethods[apiFieldType]!!
      return when (apiFieldType) {
        com.apollographql.apollo.api.Field.Type.CUSTOM -> customTypeFactoryCode(schemaField, factoryMethod)
        com.apollographql.apollo.api.Field.Type.INLINE_FRAGMENT,
        com.apollographql.apollo.api.Field.Type.FRAGMENT ->
          fragmentFactoryCode(schemaField, factoryMethod, typeConditions)
        else -> genericFactoryCode(schemaField, factoryMethod)
      }
    }

    return FieldSpec
        .builder(Array<com.apollographql.apollo.api.Field>::class.java, FIELDS_VAR)
        .addModifiers(Modifier.FINAL)
        .initializer(CodeBlock
            .builder()
            .add("{\n")
            .indent()
            .add(fields
                .map { it.factoryCode() }
                .foldIndexed(CodeBlock.builder()) { i, builder, code ->
                  builder.add(if (i > 0) ",\n" else "").add(code)
                }
                .build()
            )
            .unindent()
            .add("\n}")
            .build()
        )
        .build()
  }

  private fun mapMethod(fields: List<MapperField>): MethodSpec {
    fun MapperField.readEnumCode(index: Int): CodeBlock {
      val readValueCode = CodeBlock.builder()
          .addStatement("final \$T \$L", fieldSpec.type, fieldSpec.name)
          .beginControlFlow("if (\$LStr != null)", fieldSpec.name)
          .addStatement("\$L = \$T.valueOf(\$LStr)", fieldSpec.name, fieldSpec.type, fieldSpec.name)
          .nextControlFlow("else")
          .addStatement("\$L = null", fieldSpec.name)
          .endControlFlow()
          .build()
      return CodeBlock
          .builder()
          .addStatement("final \$T \$LStr = \$L.\$L(\$L[\$L])", ClassNames.STRING, fieldSpec.name, READER_VAR,
              apiFieldValueReadMethods[com.apollographql.apollo.api.Field.Type.STRING]!!, FIELDS_VAR, index)
          .add(readValueCode)
          .build()
    }

    fun MapperField.readScalarCode(index: Int): CodeBlock {
      return CodeBlock.of("final \$T \$L = \$L.\$L(\$L[\$L]);\n", fieldSpec.type, fieldSpec.name, READER_VAR,
          apiFieldValueReadMethods[apiFieldType]!!, FIELDS_VAR, index)
    }

    fun MapperField.readCustomCode(index: Int): CodeBlock {
      return CodeBlock.of("final \$T \$L = \$L.\$L((\$T) \$L[\$L]);\n", fieldSpec.type, fieldSpec.name,
          READER_VAR, apiFieldValueReadMethods[apiFieldType]!!,
          com.apollographql.apollo.api.Field.CustomTypeField::class.java, FIELDS_VAR, index)
    }

    fun MapperField.readObjectCode(index: Int): CodeBlock {
      val readerTypeSpec = TypeSpec.anonymousClassBuilder("")
          .superclass(apiResponseFieldObjectReaderTypeName(fieldSpec.type))
          .addMethod(MethodSpec
              .methodBuilder("read")
              .addModifiers(Modifier.PUBLIC)
              .addAnnotation(Override::class.java)
              .addException(IOException::class.java)
              .returns(fieldSpec.type)
              .addParameter(READER_PARAM)
              .addStatement("return \$L.map(\$L)", (fieldSpec.type as ClassName).mapperFieldName(), READER_VAR)
              .build())
          .build()
      return CodeBlock.of("final \$T \$L = \$L.\$L(\$L[\$L], \$L);\n", fieldSpec.type, fieldSpec.name, READER_VAR,
          apiFieldValueReadMethods[apiFieldType]!!, FIELDS_VAR, index, readerTypeSpec)
    }

    fun MapperField.readScalarListCode(index: Int): CodeBlock {
      val rawFieldType = fieldSpec.type.let { if (it.isList()) it.listParamType() else it }
      val readMethod = scalarListValueReadMethods[rawFieldType] ?: "readString()"
      val readStatement = if (rawFieldType.isEnum()) {
        CodeBlock.of("return \$T.valueOf(\$L.\$L);\n", rawFieldType, READER_VAR, readMethod)
      } else {
        CodeBlock.of("return \$L.\$L;\n", READER_VAR, readMethod)
      }
      val readerTypeSpec = TypeSpec.anonymousClassBuilder("")
          .superclass(apiResponseFieldListItemReaderType(rawFieldType))
          .addMethod(MethodSpec
              .methodBuilder("read")
              .addModifiers(Modifier.PUBLIC)
              .addAnnotation(Override::class.java)
              .addException(IOException::class.java)
              .returns(rawFieldType)
              .addParameter(API_RESPONSE_FIELD_LIST_ITEM_READER_PARAM)
              .addCode(readStatement)
              .build())
          .build()
      return CodeBlock.of("final \$T \$L = \$L.\$L(\$L[\$L], \$L);\n", fieldSpec.type, fieldSpec.name, READER_VAR,
          apiFieldValueReadMethods[apiFieldType]!!, FIELDS_VAR, index, readerTypeSpec)
    }

    fun MapperField.readCustomListCode(index: Int): CodeBlock {
      val rawFieldType = fieldSpec.type.let { if (it.isList()) it.listParamType() else it }
      val customScalarEnum = CustomEnumTypeSpecBuilder.className(context)
      val customScalarEnumConst = normalizeGraphQlType(schemaField.type).toUpperCase(Locale.ENGLISH)
      val readerTypeSpec = TypeSpec.anonymousClassBuilder("")
          .superclass(apiResponseFieldListItemReaderType(rawFieldType))
          .addMethod(MethodSpec
              .methodBuilder("read")
              .addModifiers(Modifier.PUBLIC)
              .addAnnotation(Override::class.java)
              .addException(IOException::class.java)
              .returns(rawFieldType)
              .addParameter(API_RESPONSE_FIELD_LIST_ITEM_READER_PARAM)
              .addStatement("return \$L.readCustomType(\$T.\$L)", READER_VAR, customScalarEnum, customScalarEnumConst)
              .build())
          .build()
      return CodeBlock.of("final \$T \$L = \$L.\$L(\$L[\$L], \$L);\n", fieldSpec.type, fieldSpec.name, READER_VAR,
          apiFieldValueReadMethods[apiFieldType]!!, FIELDS_VAR, index, readerTypeSpec)
    }

    fun MapperField.readObjectListCode(index: Int): CodeBlock {
      val rawFieldType = fieldSpec.type.let { if (it.isList()) it.listParamType() else it }
      val objectReaderTypeSpec = TypeSpec.anonymousClassBuilder("")
          .superclass(apiResponseFieldObjectReaderTypeName(rawFieldType))
          .addMethod(MethodSpec
              .methodBuilder("read")
              .addModifiers(Modifier.PUBLIC)
              .addAnnotation(Override::class.java)
              .addException(IOException::class.java)
              .returns(rawFieldType)
              .addParameter(READER_PARAM)
              .addStatement("return \$L.map(\$L)", (rawFieldType as ClassName).mapperFieldName(), READER_VAR)
              .build())
          .build()
      val listReaderTypeSpec = TypeSpec.anonymousClassBuilder("")
          .superclass(apiResponseFieldListItemReaderType(rawFieldType))
          .addMethod(MethodSpec
              .methodBuilder("read")
              .addModifiers(Modifier.PUBLIC)
              .addAnnotation(Override::class.java)
              .addException(IOException::class.java)
              .returns(rawFieldType)
              .addParameter(API_RESPONSE_FIELD_LIST_ITEM_READER_PARAM)
              .addStatement("return \$L.readObject(\$L)", READER_VAR, objectReaderTypeSpec)
              .build())
          .build()
      return CodeBlock.of("final \$T \$L = \$L.\$L(\$L[\$L], \$L);\n", fieldSpec.type, fieldSpec.name, READER_VAR,
          apiFieldValueReadMethods[apiFieldType]!!, FIELDS_VAR, index, listReaderTypeSpec)
    }

    fun MapperField.readInlineFragmentCode(index: Int): CodeBlock {
      val readerTypeSpec = TypeSpec.anonymousClassBuilder("")
          .superclass(apiConditionalFieldReaderTypeName(fieldSpec.type))
          .addMethod(MethodSpec
              .methodBuilder("read")
              .addModifiers(Modifier.PUBLIC)
              .addAnnotation(Override::class.java)
              .addException(IOException::class.java)
              .returns(fieldSpec.type)
              .addParameter(ParameterSpec.builder(String::class.java, CONDITIONAL_TYPE_VAR).build())
              .addParameter(READER_PARAM)
              .addStatement("return \$L.map(\$L)", (fieldSpec.type as ClassName).mapperFieldName(), READER_PARAM.name)
              .build())
          .build()
      return CodeBlock.of("final \$T \$L = \$L.\$L((\$T) \$L[\$L], \$L);\n", fieldSpec.type, fieldSpec.name, READER_VAR,
          apiFieldValueReadMethods[apiFieldType]!!, com.apollographql.apollo.api.Field.ConditionalTypeField::class.java,
          FIELDS_VAR, index, readerTypeSpec)
    }

    fun MapperField.readFragmentsCode(index: Int): CodeBlock {
      val readerTypeSpec = TypeSpec.anonymousClassBuilder("")
          .superclass(apiConditionalFieldReaderTypeName(FRAGMENTS_CLASS))
          .addMethod(MethodSpec
              .methodBuilder("read")
              .addModifiers(Modifier.PUBLIC)
              .addAnnotation(Override::class.java)
              .addException(IOException::class.java)
              .returns(FRAGMENTS_CLASS)
              .addParameter(ParameterSpec.builder(String::class.java, CONDITIONAL_TYPE_VAR).build())
              .addParameter(READER_PARAM)
              .addStatement("return \$L.map(\$L, \$L)", FRAGMENTS_CLASS.mapperFieldName(), READER_PARAM.name,
                  CONDITIONAL_TYPE_VAR)
              .build())
          .build()
      return CodeBlock.of("final \$T \$L = \$L.\$L((\$T) \$L[\$L], \$L);\n", fieldSpec.type, fieldSpec.name, READER_VAR,
          apiFieldValueReadMethods[apiFieldType]!!, com.apollographql.apollo.api.Field.ConditionalTypeField::class.java,
          FIELDS_VAR, index, readerTypeSpec)
    }

    fun MapperField.readFieldValueCode(index: Int): CodeBlock {
      return when (apiFieldType) {
        com.apollographql.apollo.api.Field.Type.STRING,
        com.apollographql.apollo.api.Field.Type.INT,
        com.apollographql.apollo.api.Field.Type.LONG,
        com.apollographql.apollo.api.Field.Type.DOUBLE,
        com.apollographql.apollo.api.Field.Type.BOOLEAN -> readScalarCode(index)
        com.apollographql.apollo.api.Field.Type.CUSTOM -> readCustomCode(index)
        com.apollographql.apollo.api.Field.Type.ENUM -> readEnumCode(index)
        com.apollographql.apollo.api.Field.Type.OBJECT -> readObjectCode(index)
        com.apollographql.apollo.api.Field.Type.SCALAR_LIST -> readScalarListCode(index)
        com.apollographql.apollo.api.Field.Type.CUSTOM_LIST -> readCustomListCode(index)
        com.apollographql.apollo.api.Field.Type.OBJECT_LIST -> readObjectListCode(index)
        com.apollographql.apollo.api.Field.Type.INLINE_FRAGMENT -> readInlineFragmentCode(index)
        com.apollographql.apollo.api.Field.Type.FRAGMENT -> readFragmentsCode(index)
      }
    }

    val code = CodeBlock.builder()
        .add(fields
            .mapIndexed { i, field -> field.readFieldValueCode(i) }
            .fold(CodeBlock.builder(), CodeBlock.Builder::add)
            .build())
        .add("return new \$T(", typeClassName)
        .add(fields
            .mapIndexed { i, field -> CodeBlock.of("\$L\$L", if (i > 0) ", " else "", field.fieldSpec.name) }
            .fold(CodeBlock.builder(), CodeBlock.Builder::add)
            .build())
        .add(");\n")
        .build()
    return MethodSpec.methodBuilder("map")
        .addModifiers(Modifier.PUBLIC)
        .addAnnotation(Override::class.java)
        .addParameter(READER_PARAM)
        .addException(IOException::class.java)
        .returns(typeClassName)
        .addCode(code)
        .build()
  }

  private fun apiResponseFieldListItemReaderType(type: TypeName) =
      ParameterizedTypeName.get(API_RESPONSE_FIELD_LIST_READER_TYPE, type)

  private fun TypeName.isList() =
      (this is ParameterizedTypeName && rawType == ClassNames.LIST)

  private fun TypeName.isEnum() =
      ((this is ClassName) && context.typeDeclarations.count { it.kind == "EnumType" && it.name == simpleName() } > 0)

  private fun String.isCustomScalarType() =
      context.customTypeMap.containsKey(normalizeGraphQlType(this))

  private fun TypeName.isScalar() = (SCALAR_TYPES.contains(this) || isEnum())

  private fun TypeName.listParamType(): TypeName {
    return (this as ParameterizedTypeName)
        .typeArguments
        .first()
        .let { if (it is WildcardTypeName) it.upperBounds.first() else it }
  }

  private fun mapperFields(): List<FieldSpec> {
    return fields
        .filter { !it.type.isCustomScalarType() }
        .map { it.fieldSpec(context) }
        .plus(inlineFragments.map { it.fieldSpec(context) })
        .map { it.type.unwrapOptionalType().withoutAnnotations() }
        .map { it.let { if (it.isList()) it.listParamType() else it } }
        .filter { !it.isScalar() }
        .map { it.overrideTypeName(typeOverrideMap) as ClassName }
        .plus(if (fragmentSpreads.isEmpty()) emptyList<ClassName>() else listOf(FRAGMENTS_CLASS))
        .map {
          val mapperClassName = ClassName.get(it.packageName(), it.simpleName(), Util.MAPPER_TYPE_NAME)
          FieldSpec.builder(mapperClassName, it.mapperFieldName(), Modifier.FINAL)
              .initializer(CodeBlock.of("new \$L()", mapperClassName))
              .build()
        }
  }

  private fun apiResponseFieldObjectReaderTypeName(type: TypeName) =
      ParameterizedTypeName.get(API_RESPONSE_FIELD_OBJECT_READER_TYPE, type)

  private fun apiConditionalFieldReaderTypeName(type: TypeName) =
      ParameterizedTypeName.get(API_RESPONSE_FIELD_CONDITIONAL_TYPE_READER_TYPE, type)

  private fun apiFieldType(schemaField: Field, fieldSpec: FieldSpec): com.apollographql.apollo.api.Field.Type {
    if (fieldSpec.type.isList()) {
      val rawFieldType = fieldSpec.type.let { if (it.isList()) it.listParamType() else it }
      if (schemaField.type.isCustomScalarType()) {
        return com.apollographql.apollo.api.Field.Type.CUSTOM_LIST
      } else if (rawFieldType.isScalar()) {
        return com.apollographql.apollo.api.Field.Type.SCALAR_LIST
      } else {
        return com.apollographql.apollo.api.Field.Type.OBJECT_LIST
      }
    }

    if (schemaField.type.isCustomScalarType()) {
      return com.apollographql.apollo.api.Field.Type.CUSTOM
    }

    if (fieldSpec.type.isScalar()) {
      return when (fieldSpec.type) {
        TypeName.INT, TypeName.INT.box() -> com.apollographql.apollo.api.Field.Type.INT
        TypeName.LONG, TypeName.LONG.box() -> com.apollographql.apollo.api.Field.Type.LONG
        TypeName.DOUBLE, TypeName.DOUBLE.box() -> com.apollographql.apollo.api.Field.Type.DOUBLE
        TypeName.BOOLEAN, TypeName.BOOLEAN.box() -> com.apollographql.apollo.api.Field.Type.BOOLEAN
        else -> com.apollographql.apollo.api.Field.Type.STRING
      }
    }

    return com.apollographql.apollo.api.Field.Type.OBJECT
  }

  private class MapperField(
      val schemaField: Field,
      val fieldSpec: FieldSpec,
      val apiFieldType: com.apollographql.apollo.api.Field.Type,
      val typeConditions: List<String> = emptyList()
  )

  companion object {
    private val FRAGMENTS_FIELD = FieldSpec.builder(ClassName.get("", SchemaTypeSpecBuilder.FRAGMENTS_TYPE_NAME),
        SchemaTypeSpecBuilder.FRAGMENTS_TYPE_NAME.decapitalize()).build()
    private val FRAGMENTS_CLASS = ClassName.get("", "Fragments")
    private val FIELDS_VAR = "fields"
    private val CONDITIONAL_TYPE_VAR = "conditionalType"
    private val READER_VAR = "reader"
    private val READER_PARAM = ParameterSpec.builder(ResponseReader::class.java, READER_VAR).build()
    private val SCALAR_TYPES = listOf(ClassNames.STRING, TypeName.INT, TypeName.INT.box(), TypeName.LONG,
        TypeName.LONG.box(), TypeName.DOUBLE, TypeName.DOUBLE.box(), TypeName.BOOLEAN, TypeName.BOOLEAN.box())
    private val API_RESPONSE_FIELD_TYPE = ClassName.get(com.apollographql.apollo.api.Field::class.java)
    private val API_RESPONSE_FIELD_OBJECT_READER_TYPE = ClassName.get(
        com.apollographql.apollo.api.ResponseReader.ObjectReader::class.java)
    private val API_RESPONSE_FIELD_LIST_READER_TYPE = ClassName.get(
        com.apollographql.apollo.api.ResponseReader.ListReader::class.java)
    private val API_RESPONSE_FIELD_LIST_ITEM_READER_PARAM =
        ParameterSpec.builder(ResponseReader.ListItemReader::class.java, READER_VAR).build()
    private val API_RESPONSE_FIELD_MAPPER_TYPE = ClassName.get(ResponseFieldMapper::class.java)
    private val API_RESPONSE_FIELD_CONDITIONAL_TYPE_READER_TYPE = ClassName.get(
        com.apollographql.apollo.api.ResponseReader.ConditionalTypeReader::class.java)

    private fun normalizeGraphQlType(type: String) =
        type.removeSuffix("!").removeSurrounding(prefix = "[", suffix = "]").removeSuffix("!")
  }
}
