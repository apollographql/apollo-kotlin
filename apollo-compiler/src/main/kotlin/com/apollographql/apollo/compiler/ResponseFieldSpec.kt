package com.apollographql.apollo.compiler

import com.apollographql.apollo.api.ResponseField
import com.apollographql.apollo.api.ResponseReader
import com.apollographql.apollo.api.ResponseWriter
import com.apollographql.apollo.compiler.ir.CodeGenerationContext
import com.apollographql.apollo.compiler.ir.Condition
import com.apollographql.apollo.compiler.ir.Field
import com.apollographql.apollo.compiler.ir.TypeDeclaration
import com.squareup.javapoet.*
import java.util.*
import javax.lang.model.element.Modifier

class ResponseFieldSpec(
    val irField: Field,
    val fieldSpec: FieldSpec,
    val normalizedFieldSpec: FieldSpec,
    val responseFieldType: ResponseField.Type,
    val typeConditions: List<String> = emptyList(),
    val context: CodeGenerationContext
) {
  fun factoryCode(): CodeBlock {
    val factoryMethod = FACTORY_METHODS[responseFieldType]!!
    return when (responseFieldType) {
      ResponseField.Type.CUSTOM -> customTypeFactoryCode(irField, factoryMethod)
      ResponseField.Type.INLINE_FRAGMENT, ResponseField.Type.FRAGMENT ->
        fragmentFactoryCode(irField, factoryMethod, typeConditions)
      else -> genericFactoryCode(irField, factoryMethod)
    }
  }

  fun readValueCode(readerParam: CodeBlock, fieldParam: CodeBlock): CodeBlock {
    return when (responseFieldType) {
      ResponseField.Type.STRING,
      ResponseField.Type.INT,
      ResponseField.Type.LONG,
      ResponseField.Type.DOUBLE,
      ResponseField.Type.BOOLEAN -> readScalarCode(readerParam, fieldParam)
      ResponseField.Type.CUSTOM -> readCustomCode(readerParam, fieldParam)
      ResponseField.Type.ENUM -> readEnumCode(readerParam, fieldParam)
      ResponseField.Type.OBJECT -> readObjectCode(readerParam, fieldParam)
      ResponseField.Type.LIST -> readListCode(readerParam, fieldParam)
      ResponseField.Type.INLINE_FRAGMENT -> readInlineFragmentCode(readerParam, fieldParam)
      ResponseField.Type.FRAGMENT -> readFragmentsCode(readerParam, fieldParam)
    }
  }

  fun writeValueCode(writerParam: CodeBlock, fieldParam: CodeBlock, marshaller: CodeBlock): CodeBlock {
    return when (responseFieldType) {
      ResponseField.Type.STRING,
      ResponseField.Type.INT,
      ResponseField.Type.LONG,
      ResponseField.Type.DOUBLE,
      ResponseField.Type.BOOLEAN -> writeScalarCode(writerParam, fieldParam)
      ResponseField.Type.ENUM -> writeEnumCode(writerParam, fieldParam)
      ResponseField.Type.CUSTOM -> writeCustomCode(writerParam, fieldParam)
      ResponseField.Type.OBJECT -> writeObjectCode(writerParam, fieldParam, marshaller)
      ResponseField.Type.LIST -> writeListCode(writerParam, fieldParam, marshaller)
      ResponseField.Type.INLINE_FRAGMENT -> writeInlineFragmentCode(writerParam, marshaller)
      ResponseField.Type.FRAGMENT -> writeFragmentsCode(writerParam, marshaller)
      else -> CodeBlock.of("")
    }
  }

  private fun customTypeFactoryCode(irField: Field, factoryMethod: String): CodeBlock {
    val customScalarEnum = CustomEnumTypeSpecBuilder.className(context)
    val customScalarEnumConst = normalizeGraphQlType(irField.type).toUpperCase(Locale.ENGLISH)
    return CodeBlock.of("\$T.\$L(\$S, \$S, \$L, \$L, \$T.\$L, \$L)", ResponseField::class.java,
        factoryMethod,
        irField.responseName, irField.fieldName, irField.argumentCodeBlock(), irField.isOptional(), customScalarEnum,
        customScalarEnumConst, conditionsCodeBlock(irField))
  }

  private fun fragmentFactoryCode(irField: Field, factoryMethod: String, typeConditions: List<String>): CodeBlock {
    val typeConditionListCode = typeConditions.foldIndexed(CodeBlock.builder()) { i, builder, typeCondition ->
      builder.add(if (i > 0) ",\n" else "").add("\$S", typeCondition)
    }.let {
      CodeBlock.builder()
          .add("\$T.asList(", Arrays::class.java)
          .add(it.build())
          .add(")")
          .build()
    }
    return CodeBlock.of("\$T.\$L(\$S, \$S, \$L)", ResponseField::class.java, factoryMethod, irField.responseName,
        irField.fieldName, typeConditionListCode)
  }

  private fun genericFactoryCode(irField: Field, factoryMethod: String): CodeBlock {
    return CodeBlock.of("\$T.\$L(\$S, \$S, \$L, \$L, \$L)", ResponseField::class.java, factoryMethod,
        irField.responseName, irField.fieldName, irField.argumentCodeBlock(), irField.isOptional(),
        conditionsCodeBlock(irField))
  }

  private fun readEnumCode(readerParam: CodeBlock, fieldParam: CodeBlock): CodeBlock {
    val readValueCode = CodeBlock.builder()
        .addStatement("final \$T \$L", normalizedFieldSpec.type, fieldSpec.name)
        .beginControlFlow("if (\$LStr != null)", fieldSpec.name)
        .addStatement("\$L = \$T.\$L(\$LStr)", fieldSpec.name, normalizedFieldSpec.type,
            TypeDeclaration.ENUM_SAFE_VALUE_OF, fieldSpec.name)
        .nextControlFlow("else")
        .addStatement("\$L = null", fieldSpec.name)
        .endControlFlow()
        .build()
    return CodeBlock
        .builder()
        .addStatement("final \$T \$LStr = \$L.\$L(\$L)", ClassNames.STRING, fieldSpec.name, readerParam,
            READ_METHODS[ResponseField.Type.STRING], fieldParam)
        .add(readValueCode)
        .build()
  }

  private fun readScalarCode(readerParam: CodeBlock, fieldParam: CodeBlock): CodeBlock {
    return CodeBlock.of("final \$T \$L = \$L.\$L(\$L);\n", normalizedFieldSpec.type, fieldSpec.name, readerParam,
        READ_METHODS[responseFieldType], fieldParam)
  }

  private fun readCustomCode(readerParam: CodeBlock, fieldParam: CodeBlock): CodeBlock {
    return CodeBlock.of("final \$T \$L = \$L.\$L((\$T) \$L);\n", normalizedFieldSpec.type, fieldSpec.name, readerParam,
        READ_METHODS[responseFieldType], ResponseField.CustomTypeField::class.java, fieldParam)
  }

  private fun readObjectCode(readerParam: CodeBlock, fieldParam: CodeBlock): CodeBlock {
    val readerTypeSpec = TypeSpec.anonymousClassBuilder("")
        .superclass(responseFieldObjectReaderType(normalizedFieldSpec.type))
        .addMethod(MethodSpec
            .methodBuilder("read")
            .addModifiers(Modifier.PUBLIC)
            .addAnnotation(Override::class.java)
            .returns(normalizedFieldSpec.type)
            .addParameter(RESPONSE_READER_PARAM)
            .addStatement("return \$L.map(\$L)", (normalizedFieldSpec.type as ClassName).mapperFieldName(),
                RESPONSE_READER_PARAM.name)
            .build())
        .build()
    return CodeBlock.of("final \$T \$L = \$L.\$L(\$L, \$L);\n", normalizedFieldSpec.type, fieldSpec.name, readerParam,
        READ_METHODS[responseFieldType], fieldParam, readerTypeSpec)
  }

  private fun readListItemStatement(rawFieldType: TypeName): CodeBlock {
    fun readScalar(): CodeBlock {
      val readMethod = SCALAR_LIST_ITEM_READ_METHODS[rawFieldType] ?: "readString"
      return if (rawFieldType.isEnum(context)) {
        CodeBlock.of("return \$T.\$L(\$L.\$L());\n", rawFieldType, TypeDeclaration.ENUM_SAFE_VALUE_OF,
            RESPONSE_LIST_ITEM_READER_PARAM.name, readMethod)
      } else {
        CodeBlock.of("return \$L.\$L();\n", RESPONSE_LIST_ITEM_READER_PARAM.name, readMethod)
      }
    }

    fun readCustom(): CodeBlock {
      val customScalarEnum = CustomEnumTypeSpecBuilder.className(context)
      val customScalarEnumConst = normalizeGraphQlType(irField.type, recursive = true).toUpperCase(Locale.ENGLISH)
      return CodeBlock.of("return \$L.readCustomType(\$T.\$L);\n", RESPONSE_LIST_ITEM_READER_PARAM.name,
          customScalarEnum, customScalarEnumConst)
    }

    fun readObject(): CodeBlock {
      val objectReaderTypeSpec = TypeSpec.anonymousClassBuilder("")
          .superclass(responseFieldObjectReaderType(rawFieldType))
          .addMethod(MethodSpec
              .methodBuilder("read")
              .addModifiers(Modifier.PUBLIC)
              .addAnnotation(Override::class.java)
              .returns(rawFieldType)
              .addParameter(RESPONSE_READER_PARAM)
              .addStatement("return \$L.map(\$L)", (rawFieldType as ClassName).mapperFieldName(),
                  RESPONSE_READER_PARAM.name)
              .build())
          .build()
      return CodeBlock.of("return \$L.readObject(\$L);\n", RESPONSE_LIST_ITEM_READER_PARAM.name, objectReaderTypeSpec)
    }

    fun readList(): CodeBlock {
      val rawFieldType = rawFieldType.listParamType()
      val readItemCode = readListItemStatement(rawFieldType)
      val listItemReaderTypeSpec = TypeSpec.anonymousClassBuilder("")
          .superclass(responseFieldListItemReaderType(rawFieldType))
          .addMethod(MethodSpec
              .methodBuilder("read")
              .addModifiers(Modifier.PUBLIC)
              .addAnnotation(Override::class.java)
              .returns(rawFieldType)
              .addParameter(RESPONSE_LIST_ITEM_READER_PARAM)
              .addCode(readItemCode)
              .build())
          .build()
      return CodeBlock.of("return \$L.readList(\$L);\n", RESPONSE_LIST_ITEM_READER_PARAM.name, listItemReaderTypeSpec)
    }

    return when {
      rawFieldType.isList() -> readList()
      irField.type.isCustomScalarType(context) -> return readCustom()
      rawFieldType.isScalar(context) -> return readScalar()
      else -> return readObject()
    }
  }

  private fun readListCode(readerParam: CodeBlock, fieldParam: CodeBlock): CodeBlock {
    val rawFieldType = normalizedFieldSpec.type.listParamType()
    val readerTypeSpec = TypeSpec.anonymousClassBuilder("")
        .superclass(responseFieldListItemReaderType(rawFieldType))
        .addMethod(MethodSpec
            .methodBuilder("read")
            .addModifiers(Modifier.PUBLIC)
            .addAnnotation(Override::class.java)
            .returns(rawFieldType)
            .addParameter(RESPONSE_LIST_ITEM_READER_PARAM)
            .addCode(readListItemStatement(rawFieldType))
            .build())
        .build()
    return CodeBlock.of("final \$T \$L = \$L.\$L(\$L, \$L);\n", normalizedFieldSpec.type, fieldSpec.name, readerParam,
        READ_METHODS[responseFieldType], fieldParam, readerTypeSpec)
  }

  private fun readInlineFragmentCode(readerParam: CodeBlock, fieldParam: CodeBlock): CodeBlock {
    val readerTypeSpec = TypeSpec.anonymousClassBuilder("")
        .superclass(conditionalResponseFieldReaderType(normalizedFieldSpec.type))
        .addMethod(MethodSpec
            .methodBuilder("read")
            .addModifiers(Modifier.PUBLIC)
            .addAnnotation(Override::class.java)
            .returns(normalizedFieldSpec.type)
            .addParameter(ParameterSpec.builder(String::class.java, CONDITIONAL_TYPE_VAR).build())
            .addParameter(RESPONSE_READER_PARAM)
            .addStatement("return \$L.map(\$L)", (normalizedFieldSpec.type as ClassName).mapperFieldName(),
                RESPONSE_READER_PARAM.name)
            .build())
        .build()
    return CodeBlock.of("final \$T \$L = \$L.\$L(\$L, \$L);\n", normalizedFieldSpec.type, fieldSpec.name,
        readerParam, READ_METHODS[responseFieldType], fieldParam, readerTypeSpec)
  }

  private fun readFragmentsCode(readerParam: CodeBlock, fieldParam: CodeBlock): CodeBlock {
    val readerTypeSpec = TypeSpec.anonymousClassBuilder("")
        .superclass(conditionalResponseFieldReaderType(FRAGMENTS_CLASS))
        .addMethod(MethodSpec
            .methodBuilder("read")
            .addModifiers(Modifier.PUBLIC)
            .addAnnotation(Override::class.java)
            .returns(FRAGMENTS_CLASS)
            .addParameter(ParameterSpec.builder(String::class.java, CONDITIONAL_TYPE_VAR).build())
            .addParameter(RESPONSE_READER_PARAM)
            .addStatement("return \$L.map(\$L, \$L)", FRAGMENTS_CLASS.mapperFieldName(),
                RESPONSE_READER_PARAM.name, CONDITIONAL_TYPE_VAR)
            .build())
        .build()
    return CodeBlock.of("final \$T \$L = \$L.\$L(\$L, \$L);\n", normalizedFieldSpec.type, fieldSpec.name,
        readerParam, READ_METHODS[responseFieldType], fieldParam, readerTypeSpec)
  }

  private fun writeScalarCode(writerParam: CodeBlock, fieldParam: CodeBlock): CodeBlock {
    val valueCode = fieldSpec.type.unwrapOptionalValue(fieldSpec.name)
    return CodeBlock.of("\$L.\$L(\$L, \$L);\n", writerParam, WRITE_METHODS[responseFieldType],
        fieldParam, valueCode)
  }

  private fun writeEnumCode(writerParam: CodeBlock, fieldParam: CodeBlock): CodeBlock {
    val valueCode = fieldSpec.type.unwrapOptionalValue(fieldSpec.name) {
      CodeBlock.of("\$L.rawValue()", it)
    }
    return CodeBlock.of("\$L.\$L(\$L, \$L);\n", writerParam, WRITE_METHODS[responseFieldType],
        fieldParam, valueCode)
  }

  private fun writeCustomCode(writerParam: CodeBlock, fieldParam: CodeBlock): CodeBlock {
    val valueCode = fieldSpec.type.unwrapOptionalValue(fieldSpec.name)
    return CodeBlock.of("\$L.\$L((\$T) \$L, \$L);\n", writerParam, WRITE_METHODS[responseFieldType],
        ResponseField.CustomTypeField::class.java, fieldParam, valueCode)
  }

  private fun writeObjectCode(writerParam: CodeBlock, fieldParam: CodeBlock, marshaller: CodeBlock): CodeBlock {
    val valueCode = fieldSpec.type.unwrapOptionalValue(fieldSpec.name) {
      CodeBlock.of("\$L.\$L", it, marshaller)
    }
    return CodeBlock.of("\$L.\$L(\$L, \$L);\n", writerParam, WRITE_METHODS[responseFieldType], fieldParam, valueCode)
  }

  private fun writeListItemStatement(listItemType: TypeName, marshaller: CodeBlock): CodeBlock {
    fun writeScalar(): CodeBlock {
      val writeMethod = SCALAR_LIST_ITEM_WRITE_METHODS[listItemType] ?: "writeString"
      return CodeBlock.builder().let {
        if (listItemType.isEnum(context)) {
          it.addStatement("\$L.\$L(((\$T) \$L).rawValue())", RESPONSE_LIST_ITEM_WRITER_PARAM.name, writeMethod,
              listItemType, ITEM_VALUE_PARAM.name)
        } else {
          it.addStatement(
              "\$L.\$L((\$T) \$L)", RESPONSE_LIST_ITEM_WRITER_PARAM.name, writeMethod, listItemType,
              ITEM_VALUE_PARAM.name
          )
        }
      }.build()
    }

    fun writeCustom(): CodeBlock {
      val customScalarEnum = CustomEnumTypeSpecBuilder.className(context)
      val customScalarEnumConst = normalizeGraphQlType(irField.type, recursive = true).toUpperCase(Locale.ENGLISH)
      return CodeBlock.builder()
          .addStatement("\$L.writeCustom(\$T.\$L, \$L)", RESPONSE_LIST_ITEM_WRITER_PARAM.name,
              customScalarEnum, customScalarEnumConst, ITEM_VALUE_PARAM.name)
          .build()
    }

    fun writeObject(): CodeBlock {
      return CodeBlock.builder()
          .addStatement("\$L.writeObject(((\$T) \$L).\$L)", RESPONSE_LIST_ITEM_WRITER_PARAM.name, listItemType,
              ITEM_VALUE_PARAM.name, marshaller)
          .build()
    }

    fun writeList(): CodeBlock {
      val rawFieldType = listItemType.listParamType()
      val readItemCode = writeListItemStatement(rawFieldType, marshaller)
      val listWriterType = TypeSpec.anonymousClassBuilder("")
          .addSuperinterface(ResponseWriter.ListWriter::class.java)
          .addMethod(MethodSpec
              .methodBuilder("write")
              .addModifiers(Modifier.PUBLIC)
              .addAnnotation(Override::class.java)
              .addParameter(ITEMS_VALUE_PARAM)
              .addParameter(RESPONSE_LIST_ITEM_WRITER_PARAM)
              .addCode(readItemCode)
              .build())
          .build()
      return CodeBlock.builder()
          .addStatement("\$L.writeList((\$T) \$L, \$L)", RESPONSE_LIST_ITEM_WRITER_PARAM.name, ClassNames.LIST,
              ITEM_VALUE_PARAM.name, listWriterType)
          .build()
    }

    return when {
      listItemType.isList() -> writeList()
      irField.type.isCustomScalarType(context) -> writeCustom()
      listItemType.isScalar(context) -> writeScalar()
      else -> writeObject()
    }.let {
      CodeBlock.builder()
          .beginControlFlow("for (Object \$L : \$L)", ITEM_VALUE_PARAM.name, ITEMS_VALUE_PARAM.name)
          .add(it)
          .endControlFlow()
          .build()
    }
  }

  private fun writeListCode(writerParam: CodeBlock, fieldParam: CodeBlock, marshaller: CodeBlock): CodeBlock {
    val listItemType = normalizedFieldSpec.type.listParamType()
    val listWriterType = TypeSpec.anonymousClassBuilder("")
        .addSuperinterface(ResponseWriter.ListWriter::class.java)
        .addMethod(MethodSpec.methodBuilder("write")
            .addModifiers(Modifier.PUBLIC)
            .addAnnotation(Override::class.java)
            .addParameter(ITEMS_VALUE_PARAM)
            .addParameter(RESPONSE_LIST_ITEM_WRITER_PARAM)
            .addCode(writeListItemStatement(listItemType, marshaller))
            .build()
        )
        .build()
    return CodeBlock.of("\$L.\$L(\$L, \$L, \$L);\n", writerParam, WRITE_METHODS[responseFieldType], fieldParam,
        fieldSpec.type.unwrapOptionalValue(fieldSpec.name), listWriterType)
  }

  private fun writeInlineFragmentCode(writerParam: CodeBlock, marshaller: CodeBlock): CodeBlock {
    return CodeBlock.builder()
        .addStatement("final \$T \$L = \$L", fieldSpec.type.unwrapOptionalType().withoutAnnotations(),
            "\$${fieldSpec.name}", fieldSpec.type.unwrapOptionalValue(fieldSpec.name))
        .beginControlFlow("if (\$L != null)", "\$${fieldSpec.name}")
        .addStatement("\$L.\$L.marshal(\$L)", "\$${fieldSpec.name}", marshaller, writerParam)
        .endControlFlow()
        .build()
  }

  private fun writeFragmentsCode(writerParam: CodeBlock, marshaller: CodeBlock): CodeBlock {
    return CodeBlock.of("\$L.\$L.marshal(\$L);\n", fieldSpec.name, marshaller, writerParam)
  }

  private fun responseFieldObjectReaderType(type: TypeName) =
      ParameterizedTypeName.get(ClassName.get(ResponseReader.ObjectReader::class.java), type)

  private fun conditionalResponseFieldReaderType(type: TypeName) =
      ParameterizedTypeName.get(ClassName.get(ResponseReader.ConditionalTypeReader::class.java), type)

  private fun responseFieldListItemReaderType(type: TypeName) =
      ParameterizedTypeName.get(ClassName.get(ResponseReader.ListReader::class.java), type)

  private fun conditionsCodeBlock(irField: Field): CodeBlock {
    val conditions = irField.conditions.let {
      if (irField.isConditional) it ?: emptyList() else emptyList()
    }.filter { it.kind == Condition.Kind.BOOLEAN.rawValue }

    if (conditions.isEmpty()) {
      return CodeBlock.of("\$T.<\$T>emptyList()", Collections::class.java, ResponseField.Condition::class.java)
    }

    return conditions.map {
      CodeBlock.of("\$T.booleanCondition(\$S, \$L)", ResponseField.Condition::class.java, it.variableName, it.inverted)
    }.foldIndexed(CodeBlock.builder()) { index, builder, codeBlock ->
      builder.add("\$L", if (index > 0) ", " else "").add(codeBlock)
    }.let {
      CodeBlock.builder()
          .add("\$T.<\$T>asList(", Arrays::class.java, ResponseField.Condition::class.java)
          .add(it.build())
          .add(")")
          .build()
    }
  }

  companion object {
    private val FACTORY_METHODS = mapOf(
        ResponseField.Type.STRING to "forString",
        ResponseField.Type.INT to "forInt",
        ResponseField.Type.LONG to "forLong",
        ResponseField.Type.DOUBLE to "forDouble",
        ResponseField.Type.BOOLEAN to "forBoolean",
        ResponseField.Type.ENUM to "forString",
        ResponseField.Type.OBJECT to "forObject",
        ResponseField.Type.LIST to "forList",
        ResponseField.Type.CUSTOM to "forCustomType",
        ResponseField.Type.FRAGMENT to "forFragment",
        ResponseField.Type.INLINE_FRAGMENT to "forInlineFragment"
    )
    private val READ_METHODS = mapOf(
        ResponseField.Type.STRING to "readString",
        ResponseField.Type.INT to "readInt",
        ResponseField.Type.LONG to "readLong",
        ResponseField.Type.DOUBLE to "readDouble",
        ResponseField.Type.BOOLEAN to "readBoolean",
        ResponseField.Type.ENUM to "readString",
        ResponseField.Type.OBJECT to "readObject",
        ResponseField.Type.LIST to "readList",
        ResponseField.Type.CUSTOM to "readCustomType",
        ResponseField.Type.FRAGMENT to "readConditional",
        ResponseField.Type.INLINE_FRAGMENT to "readConditional"
    )
    private val WRITE_METHODS = mapOf(
        ResponseField.Type.STRING to "writeString",
        ResponseField.Type.INT to "writeInt",
        ResponseField.Type.LONG to "writeLong",
        ResponseField.Type.DOUBLE to "writeDouble",
        ResponseField.Type.BOOLEAN to "writeBoolean",
        ResponseField.Type.ENUM to "writeString",
        ResponseField.Type.CUSTOM to "writeCustom",
        ResponseField.Type.OBJECT to "writeObject",
        ResponseField.Type.LIST to "writeList"
    )
    private val SCALAR_LIST_ITEM_READ_METHODS = mapOf(
        ClassNames.STRING to "readString",
        TypeName.INT to "readInt",
        TypeName.INT.box() to "readInt",
        TypeName.LONG to "readLong",
        TypeName.LONG.box() to "readLong",
        TypeName.DOUBLE to "readDouble",
        TypeName.DOUBLE.box() to "readDouble",
        TypeName.BOOLEAN to "readBoolean",
        TypeName.BOOLEAN.box() to "readBoolean"
    )
    private val SCALAR_LIST_ITEM_WRITE_METHODS = mapOf(
        ClassNames.STRING to "writeString",
        TypeName.INT to "writeInt",
        TypeName.INT.box() to "writeInt",
        TypeName.LONG to "writeLong",
        TypeName.LONG.box() to "writeLong",
        TypeName.DOUBLE to "writeDouble",
        TypeName.DOUBLE.box() to "writeDouble",
        TypeName.BOOLEAN to "writeBoolean",
        TypeName.BOOLEAN.box() to "writeBoolean"
    )
    private val RESPONSE_READER_PARAM =
        ParameterSpec.builder(ResponseReader::class.java, "reader").build()
    private val RESPONSE_LIST_ITEM_READER_PARAM =
        ParameterSpec.builder(ResponseReader.ListItemReader::class.java, "listItemReader").build()
    private val ITEMS_VALUE_PARAM = ParameterSpec.builder(List::class.java, "items").build()
    private val ITEM_VALUE_PARAM = ParameterSpec.builder(TypeName.OBJECT, "item").build()
    private val RESPONSE_LIST_ITEM_WRITER_PARAM =
        ParameterSpec.builder(ResponseWriter.ListItemWriter::class.java, "listItemWriter").build()

    private val FRAGMENTS_CLASS = ClassName.get("", "Fragments")
    private val CONDITIONAL_TYPE_VAR = "conditionalType"
  }
}