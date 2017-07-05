package com.apollographql.apollo.compiler

import com.apollographql.apollo.api.ResponseField
import com.apollographql.apollo.api.ResponseReader
import com.apollographql.apollo.api.ResponseWriter
import com.apollographql.apollo.compiler.ir.CodeGenerationContext
import com.apollographql.apollo.compiler.ir.Field
import com.squareup.javapoet.*
import java.io.IOException
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
      ResponseField.Type.INLINE_FRAGMENT,
      ResponseField.Type.FRAGMENT ->
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
      ResponseField.Type.SCALAR_LIST -> readScalarListCode(readerParam, fieldParam)
      ResponseField.Type.CUSTOM_LIST -> readCustomListCode(readerParam, fieldParam)
      ResponseField.Type.OBJECT_LIST -> readObjectListCode(readerParam, fieldParam)
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
      ResponseField.Type.SCALAR_LIST -> writeScalarList(writerParam, fieldParam)
      ResponseField.Type.CUSTOM_LIST -> writeCustomList(writerParam, fieldParam)
      ResponseField.Type.OBJECT_LIST -> writeObjectList(writerParam, fieldParam, marshaller)
      ResponseField.Type.INLINE_FRAGMENT -> writeInlineFragmentCode(writerParam, marshaller)
      ResponseField.Type.FRAGMENT -> writeFragmentsCode(writerParam, marshaller)
      else -> CodeBlock.of("")
    }
  }

  private fun customTypeFactoryCode(irField: Field, factoryMethod: String): CodeBlock {
    val customScalarEnum = CustomEnumTypeSpecBuilder.className(context)
    val customScalarEnumConst = normalizeGraphQlType(irField.type).toUpperCase(Locale.ENGLISH)
    return CodeBlock.of("\$T.\$L(\$S, \$S, \$L, \$L, \$T.\$L)", ResponseField::class.java, factoryMethod,
        irField.responseName, irField.fieldName, irField.argumentCodeBlock(), irField.isOptional(), customScalarEnum,
        customScalarEnumConst)
  }

  private fun fragmentFactoryCode(irField: Field, factoryMethod: String, typeConditions: List<String>): CodeBlock {
    val typeConditionListCode = typeConditions
        .foldIndexed(CodeBlock.builder().add("\$T.asList(", Arrays::class.java)) { i, builder, typeCondition ->
          builder.add(if (i > 0) ",\n" else "").add("\$S", typeCondition)
        }
        .add(")")
        .build()
    return CodeBlock.of("\$T.\$L(\$S, \$S, \$L)", ResponseField::class.java, factoryMethod, irField.responseName,
        irField.fieldName, typeConditionListCode)
  }

  private fun genericFactoryCode(irField: Field, factoryMethod: String): CodeBlock {
    return CodeBlock.of("\$T.\$L(\$S, \$S, \$L, \$L)", ResponseField::class.java, factoryMethod, irField.responseName,
        irField.fieldName, irField.argumentCodeBlock(), irField.isOptional())
  }

  private fun readEnumCode(readerParam: CodeBlock, fieldParam: CodeBlock): CodeBlock {
    val readValueCode = CodeBlock.builder()
        .addStatement("final \$T \$L", normalizedFieldSpec.type, fieldSpec.name)
        .beginControlFlow("if (\$LStr != null)", fieldSpec.name)
        .addStatement("\$L = \$T.valueOf(\$LStr)", fieldSpec.name, normalizedFieldSpec.type, fieldSpec.name)
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
        .superclass(responseFieldObjectReaderTypeName(normalizedFieldSpec.type))
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

  private fun readScalarListCode(readerParam: CodeBlock, fieldParam: CodeBlock): CodeBlock {
    val rawFieldType = with(normalizedFieldSpec.type) { if (isList()) listParamType() else this }
    val readMethod = SCALAR_LIST_ITEM_READ_METHODS[rawFieldType] ?: "readString"
    val readStatement = if (rawFieldType.isEnum(context)) {
      CodeBlock.of("return \$T.valueOf(\$L.\$L());\n", rawFieldType, RESPONSE_LIST_ITEM_READER_PARAM.name, readMethod)
    } else {
      CodeBlock.of("return \$L.\$L();\n", RESPONSE_LIST_ITEM_READER_PARAM.name, readMethod)
    }
    val readerTypeSpec = TypeSpec.anonymousClassBuilder("")
        .superclass(responseFieldListItemReaderType(rawFieldType))
        .addMethod(MethodSpec
            .methodBuilder("read")
            .addModifiers(Modifier.PUBLIC)
            .addAnnotation(Override::class.java)
            .returns(rawFieldType)
            .addParameter(RESPONSE_LIST_ITEM_READER_PARAM)
            .addCode(readStatement)
            .build())
        .build()
    return CodeBlock.of("final \$T \$L = \$L.\$L(\$L, \$L);\n", normalizedFieldSpec.type, fieldSpec.name, readerParam,
        READ_METHODS[responseFieldType], fieldParam, readerTypeSpec)
  }

  private fun readCustomListCode(readerParam: CodeBlock, fieldParam: CodeBlock): CodeBlock {
    val rawFieldType = normalizedFieldSpec.type.let { if (it.isList()) it.listParamType() else it }
    val customScalarEnum = CustomEnumTypeSpecBuilder.className(context)
    val customScalarEnumConst = normalizeGraphQlType(irField.type).toUpperCase(Locale.ENGLISH)
    val readerTypeSpec = TypeSpec.anonymousClassBuilder("")
        .superclass(responseFieldListItemReaderType(rawFieldType))
        .addMethod(MethodSpec
            .methodBuilder("read")
            .addModifiers(Modifier.PUBLIC)
            .addAnnotation(Override::class.java)
            .returns(rawFieldType)
            .addParameter(RESPONSE_LIST_ITEM_READER_PARAM)
            .addStatement("return \$L.readCustomType(\$T.\$L)", RESPONSE_LIST_ITEM_READER_PARAM.name, customScalarEnum,
                customScalarEnumConst)
            .build())
        .build()
    return CodeBlock.of("final \$T \$L = \$L.\$L(\$L, \$L);\n", normalizedFieldSpec.type, fieldSpec.name, readerParam,
        READ_METHODS[responseFieldType], fieldParam, readerTypeSpec)
  }

  private fun readObjectListCode(readerParam: CodeBlock, fieldParam: CodeBlock): CodeBlock {
    val rawFieldType = normalizedFieldSpec.type.let { if (it.isList()) it.listParamType() else it }
    val objectReaderTypeSpec = TypeSpec.anonymousClassBuilder("")
        .superclass(responseFieldObjectReaderTypeName(rawFieldType))
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
    val listReaderTypeSpec = TypeSpec.anonymousClassBuilder("")
        .superclass(responseFieldListItemReaderType(rawFieldType))
        .addMethod(MethodSpec
            .methodBuilder("read")
            .addModifiers(Modifier.PUBLIC)
            .addAnnotation(Override::class.java)
            .returns(rawFieldType)
            .addParameter(RESPONSE_LIST_ITEM_READER_PARAM)
            .addStatement("return \$L.readObject(\$L)", RESPONSE_LIST_ITEM_READER_PARAM.name, objectReaderTypeSpec)
            .build())
        .build()
    return CodeBlock.of("final \$T \$L = \$L.\$L(\$L, \$L);\n", normalizedFieldSpec.type, fieldSpec.name, readerParam,
        READ_METHODS[responseFieldType], fieldParam, listReaderTypeSpec)
  }

  private fun readInlineFragmentCode(readerParam: CodeBlock, fieldParam: CodeBlock): CodeBlock {
    val readerTypeSpec = TypeSpec.anonymousClassBuilder("")
        .superclass(conditionalResponseFieldReaderTypeName(normalizedFieldSpec.type))
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
    return CodeBlock.of("final \$T \$L = \$L.\$L((\$T) \$L, \$L);\n", normalizedFieldSpec.type, fieldSpec.name,
        readerParam, READ_METHODS[responseFieldType], ResponseField.ConditionalTypeField::class.java, fieldParam,
        readerTypeSpec)
  }

  private fun readFragmentsCode(readerParam: CodeBlock, fieldParam: CodeBlock): CodeBlock {
    val readerTypeSpec = TypeSpec.anonymousClassBuilder("")
        .superclass(conditionalResponseFieldReaderTypeName(FRAGMENTS_CLASS))
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
    return CodeBlock.of("final \$T \$L = \$L.\$L((\$T) \$L, \$L);\n", normalizedFieldSpec.type, fieldSpec.name,
        readerParam, READ_METHODS[responseFieldType], ResponseField.ConditionalTypeField::class.java, fieldParam,
        readerTypeSpec)
  }

  private fun writeScalarCode(writerParam: CodeBlock, fieldParam: CodeBlock): CodeBlock {
    val valueCode = fieldSpec.type.unwrapOptionalValue(fieldSpec.name)
    return CodeBlock.of("\$L.\$L(\$L, \$L);\n", writerParam, WRITE_METHODS[responseFieldType],
        fieldParam, valueCode)
  }

  private fun writeEnumCode(writerParam: CodeBlock, fieldParam: CodeBlock): CodeBlock {
    val valueCode = fieldSpec.type.unwrapOptionalValue(fieldSpec.name) {
      CodeBlock.of("\$L.name()", it)
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

  private fun writeScalarList(writerParam: CodeBlock, fieldParam: CodeBlock): CodeBlock {
    val rawFieldType = with(normalizedFieldSpec.type) { if (isList()) listParamType() else this }
    val writeMethod = SCALAR_LIST_ITEM_WRITE_METHODS[rawFieldType] ?: "writeString"
    val writeStatement = CodeBlock.builder()
        .beginControlFlow("for (\$T \$L : \$L)", rawFieldType, "\$item",
            fieldSpec.type.unwrapOptionalValue(fieldSpec.name, false))
        .add(
            if (rawFieldType.isEnum(context)) {
              CodeBlock.of("\$L.\$L(\$L.name());\n", RESPONSE_LIST_ITEM_WRITER_PARAM.name, writeMethod, "\$item")
            } else {
              CodeBlock.of("\$L.\$L(\$L);\n", RESPONSE_LIST_ITEM_WRITER_PARAM.name, writeMethod, "\$item")
            })
        .endControlFlow()
        .build()
    val listWriterType = TypeSpec.anonymousClassBuilder("")
        .addSuperinterface(ResponseWriter.ListWriter::class.java)
        .addMethod(MethodSpec.methodBuilder("write")
            .addModifiers(Modifier.PUBLIC)
            .addAnnotation(Override::class.java)
            .addParameter(RESPONSE_LIST_ITEM_WRITER_PARAM)
            .addCode(writeStatement)
            .build()
        )
        .build()
    val valueCode = fieldSpec.type.unwrapOptionalValue(fieldSpec.name) {
      CodeBlock.of("\$L", listWriterType)
    }
    return CodeBlock.of("\$L.\$L(\$L, \$L);\n", writerParam, WRITE_METHODS[responseFieldType], fieldParam, valueCode)
  }

  private fun writeCustomList(writerParam: CodeBlock, fieldParam: CodeBlock): CodeBlock {
    val rawFieldType = normalizedFieldSpec.type.let { if (it.isList()) it.listParamType() else it }
    val customScalarEnum = CustomEnumTypeSpecBuilder.className(context)
    val customScalarEnumConst = normalizeGraphQlType(irField.type).toUpperCase(Locale.ENGLISH)
    val writeStatement = CodeBlock.builder()
        .beginControlFlow("for (\$T \$L : \$L)", rawFieldType, "\$item",
            fieldSpec.type.unwrapOptionalValue(fieldSpec.name, false))
        .addStatement("\$L.writeCustom(\$T.\$L, \$L)", RESPONSE_LIST_ITEM_WRITER_PARAM.name,
            customScalarEnum, customScalarEnumConst, "\$item")
        .endControlFlow()
        .build()
    val listWriterType = TypeSpec.anonymousClassBuilder("")
        .addSuperinterface(ResponseWriter.ListWriter::class.java)
        .addMethod(MethodSpec.methodBuilder("write")
            .addModifiers(Modifier.PUBLIC)
            .addAnnotation(Override::class.java)
            .addParameter(RESPONSE_LIST_ITEM_WRITER_PARAM)
            .addCode(writeStatement)
            .build()
        )
        .build()
    val valueCode = fieldSpec.type.unwrapOptionalValue(fieldSpec.name) {
      CodeBlock.of("\$L", listWriterType)
    }
    return CodeBlock.of("\$L.\$L(\$L, \$L);\n", writerParam, WRITE_METHODS[responseFieldType], fieldParam, valueCode)
  }

  private fun writeObjectList(writerParam: CodeBlock, fieldParam: CodeBlock, marshaller: CodeBlock): CodeBlock {
    val rawFieldType = with(normalizedFieldSpec.type) { if (isList()) listParamType() else this }
    val writeStatement = CodeBlock.builder()
        .beginControlFlow("for (\$T \$L : \$L)", rawFieldType, "\$item",
            fieldSpec.type.unwrapOptionalValue(fieldSpec.name, false))
        .addStatement("\$L.writeObject(\$L.\$L)", RESPONSE_LIST_ITEM_WRITER_PARAM.name, "\$item", marshaller)
        .endControlFlow()
        .build()
    val listWriterType = TypeSpec.anonymousClassBuilder("")
        .addSuperinterface(ResponseWriter.ListWriter::class.java)
        .addMethod(MethodSpec.methodBuilder("write")
            .addModifiers(Modifier.PUBLIC)
            .addAnnotation(Override::class.java)
            .addParameter(RESPONSE_LIST_ITEM_WRITER_PARAM)
            .addCode(writeStatement)
            .build()
        )
        .build()
    val valueCode = fieldSpec.type.unwrapOptionalValue(fieldSpec.name) {
      CodeBlock.of("\$L", listWriterType)
    }
    return CodeBlock.of("\$L.\$L(\$L, \$L);\n", writerParam, WRITE_METHODS[responseFieldType], fieldParam, valueCode)
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

  private fun responseFieldObjectReaderTypeName(type: TypeName) =
      ParameterizedTypeName.get(ClassName.get(ResponseReader.ObjectReader::class.java), type)

  private fun conditionalResponseFieldReaderTypeName(type: TypeName) =
      ParameterizedTypeName.get(ClassName.get(ResponseReader.ConditionalTypeReader::class.java), type)

  private fun responseFieldListItemReaderType(type: TypeName) =
      ParameterizedTypeName.get(ClassName.get(ResponseReader.ListReader::class.java), type)

  companion object {
    private val FACTORY_METHODS = mapOf(
        ResponseField.Type.STRING to "forString",
        ResponseField.Type.INT to "forInt",
        ResponseField.Type.LONG to "forLong",
        ResponseField.Type.DOUBLE to "forDouble",
        ResponseField.Type.BOOLEAN to "forBoolean",
        ResponseField.Type.ENUM to "forString",
        ResponseField.Type.OBJECT to "forObject",
        ResponseField.Type.SCALAR_LIST to "forScalarList",
        ResponseField.Type.CUSTOM_LIST to "forCustomList",
        ResponseField.Type.OBJECT_LIST to "forObjectList",
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
        ResponseField.Type.SCALAR_LIST to "readList",
        ResponseField.Type.CUSTOM_LIST to "readList",
        ResponseField.Type.OBJECT_LIST to "readList",
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
        ResponseField.Type.SCALAR_LIST to "writeList",
        ResponseField.Type.CUSTOM_LIST to "writeList",
        ResponseField.Type.OBJECT_LIST to "writeList"
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
        ParameterSpec.builder(ResponseReader.ListItemReader::class.java, "reader").build()
    private val RESPONSE_LIST_ITEM_WRITER_PARAM =
        ParameterSpec.builder(ResponseWriter.ListItemWriter::class.java, "listItemWriter").build()

    private val FRAGMENTS_CLASS = ClassName.get("", "Fragments")
    private val CONDITIONAL_TYPE_VAR = "conditionalType"
  }
}