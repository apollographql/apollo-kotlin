package com.apollographql.apollo.compiler

import com.apollographql.apollo.compiler.ir.TypeDeclaration
import com.squareup.javapoet.*
import java.util.*
import javax.lang.model.element.Modifier

class BuilderTypeSpecBuilder(
    val targetObjectClassName: ClassName,
    val fields: List<Pair<String, TypeName>>,
    val fieldDefaultValues: Map<String, Any?>,
    val fieldJavaDocs: Map<String, String>,
    val typeDeclarations: List<TypeDeclaration>
) {
  fun build(): TypeSpec {
    return TypeSpec.classBuilder(builderClass)
        .addModifiers(Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
        .addBuilderFields()
        .addMethod(MethodSpec.constructorBuilder().build())
        .addBuilderMethods()
        .addBuilderBuildMethod()
        .build()
  }

  private fun TypeSpec.Builder.addBuilderFields(): TypeSpec.Builder {
    fun valueCode(value: Any, type: TypeName): CodeBlock = when {
      value is Number -> CodeBlock.of("\$L", value.castTo(type))
      type.isEnum(typeDeclarations) -> CodeBlock.of("\$T.\$L", type, value)
      value !is String -> CodeBlock.of("\$L", value)
      else -> CodeBlock.of("\$S", value)
    }

    fun listInitializerCode(values: List<*>, type: TypeName): CodeBlock {
      val codeBuilder = CodeBlock.builder().add("\$T.<\$T>asList(", Arrays::class.java, type)
      return values
          .filterNotNull()
          .map { valueCode(it, type) }
          .foldIndexed(codeBuilder) { index, builder, code ->
            builder.add(if (index > 0) ", " else "").add(code)
          }
          .add(")")
          .build()
    }

    return addFields(fields.map { (fieldName, fieldType) ->
      val rawFieldType = fieldType.unwrapOptionalType(true).let {
        if (it.isList()) it.listParamType() else it
      }
      val initializer = fieldDefaultValues[fieldName]
          ?.let { value ->
            when (value) {
              is List<*> -> listInitializerCode(value, rawFieldType)
              else -> valueCode(value, rawFieldType)
            }
          }
          ?.let { code ->
            fieldType.wrapOptionalValue(code)
          }
      FieldSpec.builder(fieldType, fieldName)
          .addModifiers(Modifier.PRIVATE)
          .initializer(initializer ?: fieldType.defaultOptionalValue())
          .build()
    })
  }

  private fun TypeSpec.Builder.addBuilderMethods(): TypeSpec.Builder {
    return addMethods(fields.map { (fieldName, fieldType) ->
      val javaDoc = fieldJavaDocs[fieldName]
      MethodSpec.methodBuilder(fieldName)
          .addModifiers(Modifier.PUBLIC)
          .addParameter(ParameterSpec.builder(fieldType.unwrapOptionalType(), fieldName).build())
          .let {
            if (!javaDoc.isNullOrBlank())
              it.addJavadoc(CodeBlock.of("\$L\n", javaDoc))
            else
              it
          }
          .returns(builderClass)
          .addStatement("this.\$L = \$L", fieldName, fieldType.wrapOptionalValue(CodeBlock.of("\$L", fieldName)))
          .addStatement("return this")
          .build()
    })
  }

  private fun TypeSpec.Builder.addBuilderBuildMethod(): TypeSpec.Builder {
    val validationCodeBuilder = fields.filter { (_, fieldType) ->
      !fieldType.isPrimitive && fieldType.annotations.contains(Annotations.NONNULL)
    }.map { (fieldName, _) ->
      CodeBlock.of("\$T.checkNotNull(\$L, \$S);\n", ClassNames.API_UTILS, fieldName, "$fieldName == null")
    }.fold(CodeBlock.builder(), CodeBlock.Builder::add)

    return addMethod(MethodSpec
        .methodBuilder("build")
        .addModifiers(Modifier.PUBLIC)
        .returns(targetObjectClassName)
        .addCode(validationCodeBuilder.build())
        .addStatement("return new \$T\$L", targetObjectClassName,
            fields.map { it.first }.joinToString(prefix = "(", separator = ", ", postfix = ")"))
        .build())
  }

  companion object {
    val CLASS_NAME: String = "Builder"
    private val builderClass = ClassName.get("", CLASS_NAME)

    fun builderFactoryMethod(): MethodSpec {
      return MethodSpec
          .methodBuilder("builder")
          .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
          .returns(builderClass)
          .addStatement("return new \$T()", builderClass)
          .build()
    }

    private fun Number.castTo(type: TypeName): Number {
      return if (type == TypeName.INT || type == TypeName.INT.box()) {
        toInt()
      } else if (type == TypeName.FLOAT || type == TypeName.FLOAT.box()) {
        toDouble()
      } else {
        this
      }
    }

    private fun TypeName.isEnum(typeDeclarations: List<TypeDeclaration>): Boolean {
      return ((this is ClassName) && typeDeclarations.count { it.kind == "EnumType" && it.name == simpleName() } > 0)
    }
  }
}
