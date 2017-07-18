package com.apollographql.apollo.compiler

import com.apollographql.apollo.api.InputFieldWriter
import com.apollographql.apollo.compiler.ir.CodeGenerationContext
import com.squareup.javapoet.*
import java.io.IOException
import java.util.*
import javax.lang.model.element.Modifier

class InputFieldSpec(
    val type: Type,
    val name: String,
    val graphQLType: String,
    val javaType: TypeName,
    val context: CodeGenerationContext
) {

  fun writeValueCode(writerParam: CodeBlock, marshaller: CodeBlock): CodeBlock {
    return when (type) {
      Type.STRING,
      Type.INT,
      Type.LONG,
      Type.DOUBLE,
      Type.BOOLEAN -> writeScalarCode(writerParam)
      Type.ENUM -> writeEnumCode(writerParam)
      Type.CUSTOM -> writeCustomCode(writerParam)
      Type.OBJECT -> writeObjectCode(writerParam, marshaller)
      Type.SCALAR_LIST -> writeScalarList(writerParam)
      Type.CUSTOM_LIST -> writeCustomList(writerParam)
      Type.OBJECT_LIST -> writeObjectList(writerParam, marshaller)
    }
  }

  private fun writeScalarCode(writerParam: CodeBlock): CodeBlock {
    val valueCode = javaType.unwrapOptionalValue(name)
    return CodeBlock.of("\$L.\$L(\$S, \$L);\n", writerParam, WRITE_METHODS[type], name, valueCode)
  }

  private fun writeEnumCode(writerParam: CodeBlock): CodeBlock {
    val valueCode = javaType.unwrapOptionalValue(name) {
      CodeBlock.of("\$L.name()", it)
    }
    return CodeBlock.of("\$L.\$L(\$S, \$L);\n", writerParam, WRITE_METHODS[type], name, valueCode)
  }

  private fun writeCustomCode(writerParam: CodeBlock): CodeBlock {
    val customScalarEnum = CustomEnumTypeSpecBuilder.className(context)
    val customScalarEnumConst = normalizeGraphQlType(graphQLType).toUpperCase(Locale.ENGLISH)
    val valueCode = javaType.unwrapOptionalValue(name)
    return CodeBlock.of("\$L.\$L(\$S, \$L.\$L, \$L);\n", writerParam, WRITE_METHODS[type],
        name, customScalarEnum, customScalarEnumConst, valueCode)
  }

  private fun writeObjectCode(writerParam: CodeBlock, marshaller: CodeBlock): CodeBlock {
    val valueCode = javaType.unwrapOptionalValue(name) {
      CodeBlock.of("\$L.\$L", it, marshaller)
    }
    return CodeBlock.of("\$L.\$L(\$S, \$L);\n", writerParam, WRITE_METHODS[type], name, valueCode)
  }

  private fun writeScalarList(writerParam: CodeBlock): CodeBlock {
    val rawFieldType = with(javaType) { if (isList()) listParamType() else this }
    val writeMethod = SCALAR_LIST_ITEM_WRITE_METHODS[rawFieldType] ?: "writeString"
    val writeStatement = CodeBlock.builder()
        .beginControlFlow("for (\$T \$L : \$L)", rawFieldType, "\$item",
            javaType.unwrapOptionalValue(name, false))
        .add(
            if (rawFieldType.isEnum(context)) {
              CodeBlock.of("\$L.\$L(\$L.name());\n", LIST_ITEM_WRITER_PARAM.name, writeMethod, "\$item")
            } else {
              CodeBlock.of("\$L.\$L(\$L);\n", LIST_ITEM_WRITER_PARAM.name, writeMethod, "\$item")
            })
        .endControlFlow()
        .build()
    val listWriterType = TypeSpec.anonymousClassBuilder("")
        .addSuperinterface(InputFieldWriter.ListWriter::class.java)
        .addMethod(MethodSpec.methodBuilder("write")
            .addModifiers(Modifier.PUBLIC)
            .addAnnotation(Override::class.java)
            .addException(IOException::class.java)
            .addParameter(LIST_ITEM_WRITER_PARAM)
            .addCode(writeStatement)
            .build()
        )
        .build()
    val valueCode = javaType.unwrapOptionalValue(name) {
      CodeBlock.of("\$L", listWriterType)
    }
    return CodeBlock.of("\$L.\$L(\$S, \$L);\n", writerParam, WRITE_METHODS[type], name, valueCode)
  }

  private fun writeCustomList(writerParam: CodeBlock): CodeBlock {
    val rawFieldType = javaType.let { if (it.isList()) it.listParamType() else it }
    val customScalarEnum = CustomEnumTypeSpecBuilder.className(context)
    val customScalarEnumConst = normalizeGraphQlType(graphQLType).toUpperCase(Locale.ENGLISH)
    val writeStatement = CodeBlock.builder()
        .beginControlFlow("for (\$T \$L : \$L)", rawFieldType, "\$item",
            javaType.unwrapOptionalValue(name, false))
        .addStatement("\$L.writeCustom(\$T.\$L, \$L)", LIST_ITEM_WRITER_PARAM.name, customScalarEnum,
            customScalarEnumConst, "\$item")
        .endControlFlow()
        .build()
    val listWriterType = TypeSpec.anonymousClassBuilder("")
        .addSuperinterface(InputFieldWriter.ListWriter::class.java)
        .addMethod(MethodSpec.methodBuilder("write")
            .addModifiers(Modifier.PUBLIC)
            .addAnnotation(Override::class.java)
            .addParameter(LIST_ITEM_WRITER_PARAM)
            .addException(IOException::class.java)
            .addCode(writeStatement)
            .build()
        )
        .build()
    val valueCode = javaType.unwrapOptionalValue(name) {
      CodeBlock.of("\$L", listWriterType)
    }
    return CodeBlock.of("\$L.\$L(\$S, \$L);\n", writerParam, WRITE_METHODS[type], name, valueCode)
  }

  private fun writeObjectList(writerParam: CodeBlock, marshaller: CodeBlock): CodeBlock {
    val rawFieldType = with(javaType) { if (isList()) listParamType() else this }
    val writeStatement = CodeBlock.builder()
        .beginControlFlow("for (\$T \$L : \$L)", rawFieldType, "\$item",
            javaType.unwrapOptionalValue(name, false))
        .addStatement("\$L.writeObject(\$L.\$L)", LIST_ITEM_WRITER_PARAM.name, "\$item", marshaller)
        .endControlFlow()
        .build()
    val listWriterType = TypeSpec.anonymousClassBuilder("")
        .addSuperinterface(InputFieldWriter.ListWriter::class.java)
        .addMethod(MethodSpec.methodBuilder("write")
            .addModifiers(Modifier.PUBLIC)
            .addAnnotation(Override::class.java)
            .addParameter(LIST_ITEM_WRITER_PARAM)
            .addException(IOException::class.java)
            .addCode(writeStatement)
            .build()
        )
        .build()
    val valueCode = javaType.unwrapOptionalValue(name) {
      CodeBlock.of("\$L", listWriterType)
    }
    return CodeBlock.of("\$L.\$L(\$S, \$L);\n", writerParam, WRITE_METHODS[type], name, valueCode)
  }

  companion object {
    private val WRITE_METHODS = mapOf(
        Type.STRING to "writeString",
        Type.INT to "writeInt",
        Type.LONG to "writeLong",
        Type.DOUBLE to "writeDouble",
        Type.BOOLEAN to "writeBoolean",
        Type.ENUM to "writeString",
        Type.CUSTOM to "writeCustom",
        Type.OBJECT to "writeObject",
        Type.SCALAR_LIST to "writeList",
        Type.CUSTOM_LIST to "writeList",
        Type.OBJECT_LIST to "writeList"
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
    private val LIST_ITEM_WRITER_PARAM =
        ParameterSpec.builder(InputFieldWriter.ListItemWriter::class.java, "listItemWriter").build()

    fun build(name: String, graphQLType: String, context: CodeGenerationContext): InputFieldSpec {
      val javaType = JavaTypeResolver(context = context, packageName = "")
          .resolve(typeName = graphQLType, nullableValueType = NullableValueType.ANNOTATED)
      val normalizedJavaType = javaType.withoutAnnotations()
      val type = when {
        normalizedJavaType.isList() -> {
          val rawFieldType = normalizedJavaType.let { if (it.isList()) it.listParamType() else it }
          if (graphQLType.isCustomScalarType(context)) {
            Type.CUSTOM_LIST
          } else if (rawFieldType.isScalar(context)) {
            Type.SCALAR_LIST
          } else {
            Type.OBJECT_LIST
          }
        }
        graphQLType.isCustomScalarType(context) -> Type.CUSTOM
        normalizedJavaType.isScalar(context) -> {
          when (normalizedJavaType) {
            TypeName.INT, TypeName.INT.box() -> Type.INT
            TypeName.LONG, TypeName.LONG.box() -> Type.LONG
            TypeName.DOUBLE, TypeName.DOUBLE.box() -> Type.DOUBLE
            TypeName.BOOLEAN, TypeName.BOOLEAN.box() -> Type.BOOLEAN
            else -> if (normalizedJavaType.isEnum(context)) Type.ENUM else Type.STRING
          }
        }
        else -> Type.OBJECT
      }
      return InputFieldSpec(
          type = type,
          name = name,
          graphQLType = graphQLType,
          javaType = javaType,
          context = context
      )
    }
  }

  enum class Type {
    STRING,
    INT,
    LONG,
    DOUBLE,
    BOOLEAN,
    ENUM,
    OBJECT,
    SCALAR_LIST,
    CUSTOM_LIST,
    OBJECT_LIST,
    CUSTOM,
  }
}