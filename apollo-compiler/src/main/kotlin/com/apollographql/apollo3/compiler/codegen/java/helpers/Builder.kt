package com.apollographql.apollo3.compiler.codegen.java.helpers

import com.apollographql.apollo3.compiler.codegen.ClassNames
import com.apollographql.apollo3.compiler.codegen.java.JavaAnnotations
import com.apollographql.apollo3.compiler.codegen.java.JavaClassNames
import com.apollographql.apollo3.compiler.codegen.java.JavaContext
import com.apollographql.apollo3.compiler.decapitalizeFirstLetter
import com.apollographql.apollo3.compiler.defaultOptionalValue
import com.apollographql.apollo3.compiler.ir.IrEnumValue
import com.apollographql.apollo3.compiler.ir.IrListValue
import com.apollographql.apollo3.compiler.ir.IrValue
import com.apollographql.apollo3.compiler.isList
import com.apollographql.apollo3.compiler.isOptional
import com.apollographql.apollo3.compiler.listParamType
import com.apollographql.apollo3.compiler.unwrapOptionalType
import com.apollographql.apollo3.compiler.wrapOptionalValue
import com.squareup.javapoet.ClassName
import com.squareup.javapoet.CodeBlock
import com.squareup.javapoet.FieldSpec
import com.squareup.javapoet.MethodSpec
import com.squareup.javapoet.ParameterSpec
import com.squareup.javapoet.ParameterizedTypeName
import com.squareup.javapoet.TypeName
import com.squareup.javapoet.TypeSpec
import javax.lang.model.element.Modifier

internal class Builder(
  val targetObjectClassName: ClassName,
  val fields: List<Pair<String, TypeName>>,
  val fieldDefaultValues: Map<String, IrValue?>,
  val fieldJavaDocs: Map<String, String>,
  val buildableTypes: List<TypeName> = emptyList(),
  val context: JavaContext
) {
  fun build(): TypeSpec {
    return TypeSpec.classBuilder(JavaClassNames.Builder.simpleName())
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
    fun valueCode(value: IrValue, type: TypeName): CodeBlock {
      return when (value) {
        is IrEnumValue -> CodeBlock.of("\$T.safeValueOf(\$S)", type, value.value)
        else -> value.codeBlock()
      }
    }

    fun listInitializerCode(listValue: IrListValue, type: TypeName): CodeBlock {
      val codeBuilder = CodeBlock.builder().add("\$T.<\$T>asList(", JavaClassNames.Arrays, type)
      return listValue.values
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
      val initializer = fieldDefaultValues[fieldName]?.let { value ->
        when (value) {
          is IrListValue -> listInitializerCode(value, rawFieldType)
          else -> valueCode(value, rawFieldType)
        }
      }?.let { code ->
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
      .returns(JavaClassNames.Builder)
      .addStatement("this.\$L = \$L", fieldName, fieldType.wrapOptionalValue(CodeBlock.of("\$L", fieldName)))
      .addStatement("return this")
      .build()
  }

  private fun inputFieldSetterMethodSpecs(): List<MethodSpec> {
    return fields.filter { (_, fieldType) -> fieldType.isOptional(JavaClassNames.Input) }
      .map { (fieldName, fieldType) ->
        val javaDoc = fieldJavaDocs[fieldName]
        inputFieldSetterMethodSpec(fieldName, fieldType, javaDoc)
      }
  }

  private fun inputFieldSetterMethodSpec(fieldName: String, fieldType: TypeName, javaDoc: String?): MethodSpec {
    return MethodSpec.methodBuilder("${fieldName}Input")
      .addModifiers(Modifier.PUBLIC)
      .addParameter(ParameterSpec.builder(fieldType, fieldName).addAnnotation(JavaAnnotations.NonNull).build())
      .apply {
        if (!javaDoc.isNullOrBlank()) {
          addJavadoc(CodeBlock.of("\$L\n", javaDoc))
        }
      }
      .returns(JavaClassNames.Builder)
      .addStatement("this.\$L = \$T.checkFieldNotMissing(\$L, \$S)", fieldName, ClassNames.Assertions, fieldName, fieldName)
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
          JavaClassNames.Builder.simpleName(), fieldName, fieldName, TO_BUILDER_METHOD_NAME, fieldType,
          JavaClassNames.Builder.simpleName().decapitalizeFirstLetter())
        .addStatement("\$L.accept(builder)", mutatorParam.name)
        .addStatement("this.\$L = builder.build()", fieldName)
        .addStatement("return this")
        .build()
    }

    fun setListFieldCode(mutatorParam: ParameterSpec): CodeBlock {
      return CodeBlock.builder()
        .addStatement("\$T<\$T.\$L> builders = new \$T<>()", JavaClassNames.List, fieldType.listParamType(),
          JavaClassNames.Builder.simpleName(), JavaClassNames.Arrays)
        .beginControlFlow("if (this.\$L != null)", fieldName)
        .beginControlFlow("for (\$T item : this.\$L)", fieldType.listParamType(), fieldName)
        .addStatement("builders.add(item != null ? item.toBuilder() : null)")
        .endControlFlow()
        .endControlFlow()
        .addStatement("\$L.accept(builders)", mutatorParam.name)
        .addStatement("\$T<\$T> \$L = new \$T<>()", JavaClassNames.List, fieldType.listParamType(), fieldName,
          JavaClassNames.ArrayList)
        .beginControlFlow("for (\$T.\$L item : builders)", fieldType.listParamType(), JavaClassNames.Builder.simpleName())
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
      .returns(JavaClassNames.Builder)
      .addStatement("\$T.checkFieldNotMissing(\$L, \$S)", ClassNames.Assertions, mutatorParam.name, mutatorParam.name)
      .addCode(if (fieldType.isList()) setListFieldCode(mutatorParam) else setFieldCode(mutatorParam))
      .build()
  }

  private fun buildMethod(): MethodSpec {
    val validationCodeBuilder = fields.filter { (_, fieldType) ->
      !fieldType.isPrimitive && fieldType.annotations.contains(JavaAnnotations.NonNull)
    }.map { (fieldName, _) ->
      CodeBlock.of("\$T.checkFieldNotMissing(\$L, \$S);\n", ClassNames.Assertions, fieldName, fieldName)
    }.fold(CodeBlock.builder(), CodeBlock.Builder::add)

    return MethodSpec
      .methodBuilder("build")
      .addModifiers(Modifier.PUBLIC)
      .returns(targetObjectClassName)
      .addCode(validationCodeBuilder.build())
      .addStatement(
        "return new \$T\$L",
        targetObjectClassName,
        fields.joinToString(prefix = "(", separator = ", ", postfix = ")") { it.first }
      )
      .build()
  }

  companion object {
    const val TO_BUILDER_METHOD_NAME = "toBuilder"

    private fun mutatorParam(fieldType: TypeName): ParameterSpec {
      val fieldBuilderType = if (fieldType.isList()) {
        ParameterizedTypeName.get(
          JavaClassNames.List,
          ClassName.get(
            "",
            "${(fieldType.listParamType() as ClassName).simpleName()}.${JavaClassNames.Builder.simpleName()}"))
      } else {
        ClassName.get("", "${(fieldType as ClassName).simpleName()}.${JavaClassNames.Builder.simpleName()}")
      }
      return ParameterSpec.builder(
        ParameterizedTypeName.get(JavaClassNames.Mutator, fieldBuilderType),
        "mutator"
      ).addAnnotation(JavaAnnotations.NonNull).build()
    }

    fun builderFactoryMethod(): MethodSpec {
      return MethodSpec
        .methodBuilder("builder")
        .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
        .returns(JavaClassNames.Builder)
        .addStatement("return new \$T()", JavaClassNames.Builder)
        .build()
    }
  }
}
