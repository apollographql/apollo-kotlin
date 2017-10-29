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
    val typeDeclarations: List<TypeDeclaration>,
    val buildableTypes: List<TypeName> = emptyList()
) {
  fun build(): TypeSpec {
    return TypeSpec.classBuilder(ClassNames.BUILDER.simpleName())
        .addModifiers(Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
        .addFields(builderFields())
        .addMethod(MethodSpec.constructorBuilder().build())
        .addMethods(fieldSetterMethodSpecs())
        .addMethods(inputFieldSetterMethodSpecs())
        .addMethods(fieldSetterWithMutatorMethodSpecs())
        .addMethod(buildMethod())
        .build()
  }

  private fun builderFields(): List<FieldSpec> {
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

    return fields.map { (fieldName, fieldType) ->
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
    }
  }

  private fun fieldSetterMethodSpecs(): List<MethodSpec> {
    return fields.map { (fieldName, fieldType) ->
      val javaDoc = fieldJavaDocs[fieldName]
      fieldSetterMethodSpec(fieldName, fieldType, javaDoc)
    }
  }

  private fun fieldSetterMethodSpec(fieldName: String, fieldType: TypeName, javaDoc: String?): MethodSpec {
    return MethodSpec.methodBuilder(fieldName)
        .addModifiers(Modifier.PUBLIC)
        .addParameter(ParameterSpec.builder(fieldType.unwrapOptionalType(), fieldName).build())
        .apply {
          if (!javaDoc.isNullOrBlank()) {
            addJavadoc(CodeBlock.of("\$L\n", javaDoc))
          }
        }
        .returns(ClassNames.BUILDER)
        .addStatement("this.\$L = \$L", fieldName, fieldType.wrapOptionalValue(CodeBlock.of("\$L", fieldName)))
        .addStatement("return this")
        .build()
  }

  private fun inputFieldSetterMethodSpecs(): List<MethodSpec> {
    return fields.filter { (_, fieldType) -> fieldType.isOptional(ClassNames.INPUT_TYPE) }
        .map { (fieldName, fieldType) ->
          val javaDoc = fieldJavaDocs[fieldName]
          inputFieldSetterMethodSpec(fieldName, fieldType, javaDoc)
        }
  }

  private fun inputFieldSetterMethodSpec(fieldName: String, fieldType: TypeName, javaDoc: String?): MethodSpec {
    return MethodSpec.methodBuilder("${fieldName}Input")
        .addModifiers(Modifier.PUBLIC)
        .addParameter(ParameterSpec.builder(fieldType, fieldName).addAnnotation(Annotations.NONNULL).build())
        .apply {
          if (!javaDoc.isNullOrBlank()) {
            addJavadoc(CodeBlock.of("\$L\n", javaDoc))
          }
        }
        .returns(ClassNames.BUILDER)
        .addStatement("this.\$L = \$T.checkNotNull(\$L, \$S)", fieldName, ClassNames.API_UTILS, fieldName, "$fieldName == null")
        .addStatement("return this")
        .build()
  }

  private fun fieldSetterWithMutatorMethodSpecs(): List<MethodSpec> {
    return fields
        .map { (fieldName, fieldType) ->
          fieldName to fieldType.withoutAnnotations()
        }
        .filter { (_, type) ->
          if (type.isList()) {
            buildableTypes.contains(type.listParamType())
          } else {
            buildableTypes.contains(type)
          }
        }
        .map { (fieldName, fieldType) ->
          fieldSetterWithMutatorMethodSpec(fieldName, fieldType)
        }
  }

  private fun fieldSetterWithMutatorMethodSpec(fieldName: String, fieldType: TypeName): MethodSpec {
    fun setFieldCode(mutatorParam: ParameterSpec): CodeBlock {
      return CodeBlock.builder()
          .addStatement("\$T.\$L builder = this.\$L != null ? this.\$L.\$L() : \$T.\$L()", fieldType,
              ClassNames.BUILDER.simpleName(), fieldName, fieldName, TO_BUILDER_METHOD_NAME, fieldType,
              ClassNames.BUILDER.simpleName().decapitalize())
          .addStatement("\$L.accept(builder)", mutatorParam.name)
          .addStatement("this.\$L = builder.build()", fieldName)
          .addStatement("return this")
          .build()
    }

    fun setListFieldCode(mutatorParam: ParameterSpec): CodeBlock {
      return CodeBlock.builder()
          .addStatement("\$T<\$T.\$L> builders = new \$T<>()", ClassNames.LIST, fieldType.listParamType(),
              ClassNames.BUILDER.simpleName(), ClassNames.ARRAY_LIST)
          .beginControlFlow("if (this.\$L != null)", fieldName)
          .beginControlFlow("for (\$T item : this.\$L)", fieldType.listParamType(), fieldName)
          .addStatement("builders.add(item != null ? item.toBuilder() : null)")
          .endControlFlow()
          .endControlFlow()
          .addStatement("\$L.accept(builders)", mutatorParam.name)
          .addStatement("\$T<\$T> \$L = new \$T<>()", ClassNames.LIST, fieldType.listParamType(), fieldName,
              ClassNames.ARRAY_LIST)
          .beginControlFlow("for (\$T.\$L item : builders)", fieldType.listParamType(), ClassNames.BUILDER.simpleName())
          .addStatement("\$L.add(item != null ? item.build() : null)", fieldName)
          .endControlFlow()
          .addStatement("this.\$L = \$L", fieldName, fieldName)
          .addStatement("return this")
          .build()
    }

    val javaDoc = fieldJavaDocs[fieldName]
    val mutatorParam = mutatorParam(fieldType)
    return MethodSpec.methodBuilder(fieldName)
        .addModifiers(Modifier.PUBLIC)
        .addParameter(mutatorParam)
        .apply { if (!javaDoc.isNullOrBlank()) addJavadoc(CodeBlock.of("\$L\n", javaDoc)) }
        .returns(ClassNames.BUILDER)
        .addStatement("\$T.checkNotNull(\$L, \$S)", ClassNames.API_UTILS, mutatorParam.name,
            "${mutatorParam.name} == null")
        .addCode(if (fieldType.isList()) setListFieldCode(mutatorParam) else setFieldCode(mutatorParam))
        .build()
  }

  private fun buildMethod(): MethodSpec {
    val validationCodeBuilder = fields.filter { (_, fieldType) ->
      !fieldType.isPrimitive && fieldType.annotations.contains(Annotations.NONNULL)
    }.map { (fieldName, _) ->
      CodeBlock.of("\$T.checkNotNull(\$L, \$S);\n", ClassNames.API_UTILS, fieldName, "$fieldName == null")
    }.fold(CodeBlock.builder(), CodeBlock.Builder::add)

    return MethodSpec
        .methodBuilder("build")
        .addModifiers(Modifier.PUBLIC)
        .returns(targetObjectClassName)
        .addCode(validationCodeBuilder.build())
        .addStatement("return new \$T\$L", targetObjectClassName,
            fields.map { it.first }.joinToString(prefix = "(", separator = ", ", postfix = ")"))
        .build()
  }

  companion object {
    val TO_BUILDER_METHOD_NAME = "toBuilder"

    private fun mutatorParam(fieldType: TypeName): ParameterSpec {
      val fieldBuilderType = if (fieldType.isList()) {
        ParameterizedTypeName.get(ClassNames.LIST,
            ClassName.get("",
                "${(fieldType.listParamType() as ClassName).simpleName()}.${ClassNames.BUILDER.simpleName()}"))
      } else {
        ClassName.get("", "${(fieldType as ClassName).simpleName()}.${ClassNames.BUILDER.simpleName()}")
      }
      return ParameterSpec.builder(
          ParameterizedTypeName.get(ClassNames.MUTATOR, fieldBuilderType),
          "mutator"
      ).addAnnotation(Annotations.NONNULL).build()
    }

    fun builderFactoryMethod(): MethodSpec {
      return MethodSpec
          .methodBuilder("builder")
          .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
          .returns(ClassNames.BUILDER)
          .addStatement("return new \$T()", ClassNames.BUILDER)
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
