package com.apollographql.apollo.compiler

import com.apollographql.apollo.api.ResponseField
import com.apollographql.apollo.api.ResponseReader
import com.apollographql.apollo.compiler.ir.CodeGenerationContext
import com.apollographql.apollo.compiler.ir.Field
import com.squareup.javapoet.*
import java.io.IOException
import java.util.*
import javax.lang.model.element.Modifier

class ResponseFieldSpec(
    val irField: Field,
    val fieldSpec: FieldSpec,
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

  fun readValueCode(fieldsVarName: String, index: Int): CodeBlock {
    return when (responseFieldType) {
      ResponseField.Type.STRING,
      ResponseField.Type.INT,
      ResponseField.Type.LONG,
      ResponseField.Type.DOUBLE,
      ResponseField.Type.BOOLEAN -> readScalarCode(fieldsVarName, index)
      ResponseField.Type.CUSTOM -> readCustomCode(fieldsVarName, index)
      ResponseField.Type.ENUM -> readEnumCode(index, fieldsVarName)
      ResponseField.Type.OBJECT -> readObjectCode(fieldsVarName, index)
      ResponseField.Type.SCALAR_LIST -> readScalarListCode(fieldsVarName, index)
      ResponseField.Type.CUSTOM_LIST -> readCustomListCode(fieldsVarName, index)
      ResponseField.Type.OBJECT_LIST -> readObjectListCode(fieldsVarName, index)
      ResponseField.Type.INLINE_FRAGMENT -> readInlineFragmentCode(fieldsVarName, index)
      ResponseField.Type.FRAGMENT -> readFragmentsCode(fieldsVarName, index)
    }
  }

  private fun customTypeFactoryCode(irField: Field, factoryMethod: String): CodeBlock {
    val customScalarEnum = CustomEnumTypeSpecBuilder.className(context)
    val customScalarEnumConst = normalizeGraphQlType(irField.type).toUpperCase(Locale.ENGLISH)
    return CodeBlock.of("\$T.\$L(\$S, \$S, \$L, \$L, \$T.\$L)", RESPONSE_FIELD_TYPE, factoryMethod,
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
    return CodeBlock.of("\$T.\$L(\$S, \$S, \$L)", RESPONSE_FIELD_TYPE, factoryMethod, irField.responseName,
        irField.fieldName, typeConditionListCode)
  }

  private fun genericFactoryCode(irField: Field, factoryMethod: String): CodeBlock {
    return CodeBlock.of("\$T.\$L(\$S, \$S, \$L, \$L)", RESPONSE_FIELD_TYPE, factoryMethod, irField.responseName,
        irField.fieldName, irField.argumentCodeBlock(), irField.isOptional())
  }

  private fun readEnumCode(index: Int, fieldsVarName: String): CodeBlock {
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
            READ_METHODS[ResponseField.Type.STRING], fieldsVarName, index)
        .add(readValueCode)
        .build()
  }

  private fun readScalarCode(fieldsVarName: String, index: Int): CodeBlock {
    return CodeBlock.of("final \$T \$L = \$L.\$L(\$L[\$L]);\n", fieldSpec.type, fieldSpec.name, READER_VAR,
        READ_METHODS[responseFieldType], fieldsVarName, index)
  }

  private fun readCustomCode(fieldsVarName: String, index: Int): CodeBlock {
    return CodeBlock.of("final \$T \$L = \$L.\$L((\$T) \$L[\$L]);\n", fieldSpec.type, fieldSpec.name,
        READER_VAR, READ_METHODS[responseFieldType],
        ResponseField.CustomTypeField::class.java, fieldsVarName, index)
  }

  private fun readObjectCode(fieldsVarName: String, index: Int): CodeBlock {
    val readerTypeSpec = TypeSpec.anonymousClassBuilder("")
        .superclass(responseFieldObjectReaderTypeName(fieldSpec.type))
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
        READ_METHODS[responseFieldType], fieldsVarName, index, readerTypeSpec)
  }

  private fun readScalarListCode(fieldsVarName: String, index: Int): CodeBlock {
    val rawFieldType = fieldSpec.type.let { if (it.isList()) it.listParamType() else it }
    val readMethod = SCALAR_LIST_ITEM_READ_METHODS[rawFieldType] ?: "readString()"
    val readStatement = if (rawFieldType.isEnum(context)) {
      CodeBlock.of("return \$T.valueOf(\$L.\$L);\n", rawFieldType, READER_VAR, readMethod)
    } else {
      CodeBlock.of("return \$L.\$L;\n", READER_VAR, readMethod)
    }
    val readerTypeSpec = TypeSpec.anonymousClassBuilder("")
        .superclass(responseFieldListItemReaderType(rawFieldType))
        .addMethod(MethodSpec
            .methodBuilder("read")
            .addModifiers(Modifier.PUBLIC)
            .addAnnotation(Override::class.java)
            .addException(IOException::class.java)
            .returns(rawFieldType)
            .addParameter(RESPONSE_FIELD_LIST_ITEM_READER_PARAM)
            .addCode(readStatement)
            .build())
        .build()
    return CodeBlock.of("final \$T \$L = \$L.\$L(\$L[\$L], \$L);\n", fieldSpec.type, fieldSpec.name, READER_VAR,
        READ_METHODS[responseFieldType], fieldsVarName, index, readerTypeSpec)
  }

  private fun readCustomListCode(fieldsVarName: String, index: Int): CodeBlock {
    val rawFieldType = fieldSpec.type.let { if (it.isList()) it.listParamType() else it }
    val customScalarEnum = CustomEnumTypeSpecBuilder.className(context)
    val customScalarEnumConst = normalizeGraphQlType(irField.type).toUpperCase(Locale.ENGLISH)
    val readerTypeSpec = TypeSpec.anonymousClassBuilder("")
        .superclass(responseFieldListItemReaderType(rawFieldType))
        .addMethod(MethodSpec
            .methodBuilder("read")
            .addModifiers(Modifier.PUBLIC)
            .addAnnotation(Override::class.java)
            .addException(IOException::class.java)
            .returns(rawFieldType)
            .addParameter(RESPONSE_FIELD_LIST_ITEM_READER_PARAM)
            .addStatement("return \$L.readCustomType(\$T.\$L)", READER_VAR, customScalarEnum, customScalarEnumConst)
            .build())
        .build()
    return CodeBlock.of("final \$T \$L = \$L.\$L(\$L[\$L], \$L);\n", fieldSpec.type, fieldSpec.name, READER_VAR,
        READ_METHODS[responseFieldType], fieldsVarName, index, readerTypeSpec)
  }

  private fun readObjectListCode(fieldsVarName: String, index: Int): CodeBlock {
    val rawFieldType = fieldSpec.type.let { if (it.isList()) it.listParamType() else it }
    val objectReaderTypeSpec = TypeSpec.anonymousClassBuilder("")
        .superclass(responseFieldObjectReaderTypeName(rawFieldType))
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
        .superclass(responseFieldListItemReaderType(rawFieldType))
        .addMethod(MethodSpec
            .methodBuilder("read")
            .addModifiers(Modifier.PUBLIC)
            .addAnnotation(Override::class.java)
            .addException(IOException::class.java)
            .returns(rawFieldType)
            .addParameter(RESPONSE_FIELD_LIST_ITEM_READER_PARAM)
            .addStatement("return \$L.readObject(\$L)", READER_VAR, objectReaderTypeSpec)
            .build())
        .build()
    return CodeBlock.of("final \$T \$L = \$L.\$L(\$L[\$L], \$L);\n", fieldSpec.type, fieldSpec.name, READER_VAR,
        READ_METHODS[responseFieldType], fieldsVarName, index, listReaderTypeSpec)
  }

  private fun readInlineFragmentCode(fieldsVarName: String, index: Int): CodeBlock {
    val readerTypeSpec = TypeSpec.anonymousClassBuilder("")
        .superclass(conditionalResponseFieldReaderTypeName(fieldSpec.type))
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
        READ_METHODS[responseFieldType], ResponseField.ConditionalTypeField::class.java,
        fieldsVarName, index, readerTypeSpec)
  }

  private fun readFragmentsCode(fieldsVarName: String, index: Int): CodeBlock {
    val readerTypeSpec = TypeSpec.anonymousClassBuilder("")
        .superclass(conditionalResponseFieldReaderTypeName(FRAGMENTS_CLASS))
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
        READ_METHODS[responseFieldType], ResponseField.ConditionalTypeField::class.java,
        fieldsVarName, index, readerTypeSpec)
  }

  private fun responseFieldObjectReaderTypeName(type: TypeName) =
      ParameterizedTypeName.get(RESPONSE_FIELD_OBJECT_READER_TYPE, type)

  private fun conditionalResponseFieldReaderTypeName(type: TypeName) =
      ParameterizedTypeName.get(RESPONSE_FIELD_CONDITIONAL_TYPE_READER_TYPE, type)

  private fun responseFieldListItemReaderType(type: TypeName) =
      ParameterizedTypeName.get(RESPONSE_FIELD_LIST_READER_TYPE, type)

  companion object {
    private val RESPONSE_FIELD_TYPE = ClassName.get(ResponseField::class.java)
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
    private val SCALAR_LIST_ITEM_READ_METHODS = mapOf(
        TypeName.INT to "readInt()",
        TypeName.INT.box() to "readInt()",
        TypeName.LONG to "readLong()",
        TypeName.LONG.box() to "readLong()",
        TypeName.DOUBLE to "readDouble()",
        TypeName.DOUBLE.box() to "readDouble()",
        TypeName.BOOLEAN to "readBoolean()",
        TypeName.BOOLEAN.box() to "readBoolean()"
    )
    private val FRAGMENTS_CLASS = ClassName.get("", "Fragments")
    private val CONDITIONAL_TYPE_VAR = "conditionalType"
    private val READER_VAR = "reader"
    private val READER_PARAM = ParameterSpec.builder(ResponseReader::class.java, READER_VAR).build()
    private val RESPONSE_FIELD_OBJECT_READER_TYPE = ClassName.get(ResponseReader.ObjectReader::class.java)
    private val RESPONSE_FIELD_LIST_READER_TYPE = ClassName.get(ResponseReader.ListReader::class.java)
    private val RESPONSE_FIELD_LIST_ITEM_READER_PARAM =
        ParameterSpec.builder(ResponseReader.ListItemReader::class.java, READER_VAR).build()
    private val RESPONSE_FIELD_CONDITIONAL_TYPE_READER_TYPE =
        ClassName.get(ResponseReader.ConditionalTypeReader::class.java)
  }
}