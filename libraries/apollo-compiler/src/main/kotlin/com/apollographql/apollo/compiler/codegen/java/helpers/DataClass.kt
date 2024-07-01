package com.apollographql.apollo.compiler.codegen.java.helpers

import com.apollographql.apollo.compiler.GeneratedMethod
import com.apollographql.apollo.compiler.GeneratedMethod.EQUALS_HASH_CODE
import com.apollographql.apollo.compiler.GeneratedMethod.TO_STRING
import com.apollographql.apollo.compiler.codegen.Identifier.__h
import com.apollographql.apollo.compiler.codegen.java.JavaClassNames
import com.apollographql.apollo.compiler.codegen.java.L
import com.apollographql.apollo.compiler.codegen.java.joinToCode
import com.apollographql.apollo.compiler.internal.applyIf
import com.squareup.javapoet.ClassName
import com.squareup.javapoet.CodeBlock
import com.squareup.javapoet.FieldSpec
import com.squareup.javapoet.MethodSpec
import com.squareup.javapoet.ParameterSpec
import com.squareup.javapoet.TypeName
import com.squareup.javapoet.TypeSpec
import javax.lang.model.element.Modifier

/**
 * Given a list of [ParameterSpec]:
 * - add the fields matching these parameters
 * - add a constructor that initializes the properties from the parameters
 * - add a 'hashCode' implementation
 * - add a 'toString'
 *
 * This is named "data class" because it's similar to Kotlin data classes even if technically Java
 * doesn't have data classes
 */
internal fun TypeSpec.Builder.makeClassFromParameters(
    generateMethods: List<GeneratedMethod>,
    parameters: List<ParameterSpec>,
    className: ClassName
): TypeSpec.Builder {
  addMethod(
      MethodSpec.constructorBuilder()
          .addModifiers(Modifier.PUBLIC)
          .addParameters(parameters)
          .addCode(
              parameters.map { CodeBlock.of("this.$L = $L;", it.name, it.name) }.joinToCode("\n", suffix = "\n")
          )
          .build()
  )

  addFields(
      parameters.map {
        FieldSpec.builder(it.type, it.name)
            .addModifiers(Modifier.FINAL)
            .addModifiers(Modifier.PUBLIC)
            .addAnnotations(it.annotations)
            .build()
      }
  )
  return addGeneratedMethods(className, generateMethods)
}

internal fun TypeSpec.Builder.addGeneratedMethods(
    className: ClassName,
    generateMethods: List<GeneratedMethod> = listOf(EQUALS_HASH_CODE, TO_STRING)
): TypeSpec.Builder {
  return applyIf(generateMethods.contains(EQUALS_HASH_CODE)) { withEqualsImplementation(className) }
      .applyIf(generateMethods.contains(EQUALS_HASH_CODE)) { withHashCodeImplementation() }
      .applyIf(generateMethods.contains(TO_STRING)) { withToStringImplementation(className) }
}

/**
 * Same as [makeClassFromParameters] but takes fields instead of parameters as input
 */
internal fun TypeSpec.Builder.makeClassFromProperties(
    generateMethods: List<GeneratedMethod>,
    fields: List<FieldSpec>,
    className: ClassName
): TypeSpec.Builder {
  addMethod(
      MethodSpec.constructorBuilder()
          .addModifiers(Modifier.PUBLIC)
          .addParameters(
              fields.map {
                ParameterSpec
                    .builder(it.type, it.name)
                    .addAnnotations(it.annotations.filterNot { it.type == JavaClassNames.Deprecated })
                    .build()
              }
          )
          .addCode(
              fields.map { CodeBlock.of("this.$L = $L;", it.name, it.name) }.joinToCode("\n", suffix = "\n")
          )
          .build()
  )

  addFields(fields)
  return addGeneratedMethods(className, generateMethods)
}


internal fun TypeSpec.Builder.withToStringImplementation(className: ClassName): TypeSpec.Builder {
  fun printFieldCode(fieldIndex: Int, fieldName: String) =
    CodeBlock.builder()
        .let { if (fieldIndex > 0) it.add(" + \", \"\n") else it.add("\n") }
        .indent()
        .add("+ \$S + \$L", "$fieldName=", fieldName)
        .unindent()
        .build()

  fun methodCode() =
    CodeBlock.builder()
        .beginControlFlow("if (\$L == null)", MEMOIZED_TO_STRING_VAR)
        .add("\$L = \$S", "\$toString", "${className.simpleName()}{")
        .add(fieldSpecs
            .filter { !it.hasModifier(Modifier.STATIC) }
            .filter { !it.hasModifier(Modifier.TRANSIENT) }
            .map { it.name }
            .mapIndexed(::printFieldCode)
            .fold(CodeBlock.builder(), CodeBlock.Builder::add)
            .build())
        .add(CodeBlock.builder()
            .indent()
            .add("\n+ \$S;\n", "}")
            .unindent()
            .build())
        .endControlFlow()
        .addStatement("return \$L", MEMOIZED_TO_STRING_VAR)
        .build()

  return addField(FieldSpec.builder(JavaClassNames.String, MEMOIZED_TO_STRING_VAR, Modifier.PRIVATE, Modifier.VOLATILE,
      Modifier.TRANSIENT)
      .build())
      .addMethod(MethodSpec.methodBuilder("toString")
          .addAnnotation(JavaClassNames.Override)
          .addModifiers(Modifier.PUBLIC)
          .returns(JavaClassNames.String)
          .addCode(methodCode())
          .build())
}

private fun List<FieldSpec>.equalsCode(): CodeBlock = filter { !it.hasModifier(Modifier.STATIC) }
    .filter { !it.hasModifier(Modifier.TRANSIENT) }
    .map { it.equalsCode() }
    .joinToCode("\n &&")

private fun FieldSpec.equalsCode() =
  CodeBlock.builder()
      .let {
        if (type.isPrimitive) {
          if (type == TypeName.DOUBLE) {
            it.add("Double.doubleToLongBits(this.\$L) == Double.doubleToLongBits(that.\$L)",
                name, name)
          } else {
            it.add("this.\$L == that.\$L", name, name)
          }
        } else {
          it.add("((this.\$L == null) ? (that.\$L == null) : this.\$L.equals(that.\$L))", name, name, name, name)
        }
      }
      .build()

internal fun TypeSpec.Builder.withEqualsImplementation(className: ClassName): TypeSpec.Builder {
  fun methodCode(typeJavaClass: ClassName) =
    CodeBlock.builder()
        .beginControlFlow("if (o == this)")
        .addStatement("return true")
        .endControlFlow()
        .beginControlFlow("if (o instanceof \$T)", typeJavaClass)
        .apply {
          if (fieldSpecs.isEmpty()) {
            add("return true;\n")
          } else {
            addStatement("\$T that = (\$T) o", typeJavaClass, typeJavaClass)
            add("return $L;\n", if (fieldSpecs.isEmpty()) "true" else fieldSpecs.equalsCode())
          }
        }
        .endControlFlow()
        .addStatement("return false")
        .build()

  return addMethod(MethodSpec.methodBuilder("equals")
      .addAnnotation(JavaClassNames.Override)
      .addModifiers(Modifier.PUBLIC)
      .returns(TypeName.BOOLEAN)
      .addParameter(ParameterSpec.builder(TypeName.OBJECT, "o").build())
      .addCode(methodCode(className))
      .build())
}

internal fun TypeSpec.Builder.withHashCodeImplementation(): TypeSpec.Builder {
  fun hashFieldCode(field: FieldSpec) =
    CodeBlock.builder()
        .addStatement("$__h *= 1000003")
        .let {
          if (field.type.isPrimitive) {
            when (field.type.withoutAnnotations()) {
              TypeName.DOUBLE -> it.addStatement("$__h ^= Double.valueOf(\$L).hashCode()", field.name)
              TypeName.BOOLEAN -> it.addStatement("$__h ^= Boolean.valueOf(\$L).hashCode()", field.name)
              else -> it.addStatement("$__h ^= \$L", field.name)
            }
          } else {
            it.addStatement("$__h ^= (\$L == null) ? 0 : \$L.hashCode()", field.name, field.name)
          }
        }
        .build()

  fun methodCode() =
    CodeBlock.builder()
        .beginControlFlow("if (!\$L)", MEMOIZED_HASH_CODE_FLAG_VAR)
        .addStatement("int $__h = 1")
        .add(fieldSpecs
            .filter { !it.hasModifier(Modifier.STATIC) }
            .filter { !it.hasModifier(Modifier.TRANSIENT) }
            .map(::hashFieldCode)
            .fold(CodeBlock.builder(), CodeBlock.Builder::add)
            .build())
        .addStatement("\$L = $__h", MEMOIZED_HASH_CODE_VAR)
        .addStatement("\$L = true", MEMOIZED_HASH_CODE_FLAG_VAR)
        .endControlFlow()
        .addStatement("return \$L", MEMOIZED_HASH_CODE_VAR)
        .build()

  return addField(FieldSpec.builder(TypeName.INT, MEMOIZED_HASH_CODE_VAR, Modifier.PRIVATE, Modifier.VOLATILE,
      Modifier.TRANSIENT).build())
      .addField(FieldSpec.builder(TypeName.BOOLEAN, MEMOIZED_HASH_CODE_FLAG_VAR, Modifier.PRIVATE,
          Modifier.VOLATILE, Modifier.TRANSIENT).build())
      .addMethod(MethodSpec.methodBuilder("hashCode")
          .addAnnotation(JavaClassNames.Override)
          .addModifiers(Modifier.PUBLIC)
          .returns(TypeName.INT)
          .addCode(methodCode())
          .build())
}


private const val MEMOIZED_HASH_CODE_VAR: String = "\$hashCode"
private const val MEMOIZED_HASH_CODE_FLAG_VAR: String = "\$hashCodeMemoized"
private const val MEMOIZED_TO_STRING_VAR: String = "\$toString"
