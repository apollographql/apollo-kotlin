package com.apollographql.apollo3.compiler.codegen.java.file

import com.apollographql.apollo3.compiler.applyIf
import com.apollographql.apollo3.compiler.codegen.java.CodegenJavaFile
import com.apollographql.apollo3.compiler.codegen.java.JavaClassBuilder
import com.apollographql.apollo3.compiler.codegen.java.JavaContext
import com.apollographql.apollo3.compiler.codegen.java.L
import com.apollographql.apollo3.compiler.codegen.java.helpers.Builder
import com.apollographql.apollo3.compiler.codegen.java.helpers.isCustomScalar
import com.apollographql.apollo3.compiler.codegen.java.helpers.makeDataClassFromParameters
import com.apollographql.apollo3.compiler.codegen.java.helpers.toNamedType
import com.apollographql.apollo3.compiler.codegen.java.helpers.toParameterSpec
import com.apollographql.apollo3.compiler.ir.IrInputObject
import com.apollographql.apollo3.compiler.ir.IrInputObjectType
import com.squareup.javapoet.ClassName
import com.squareup.javapoet.TypeSpec
import javax.lang.model.element.Modifier

internal class InputObjectBuilder(
    val context: JavaContext,
    val inputObject: IrInputObject,
) : JavaClassBuilder {
  private val packageName = context.layout.typePackageName()
  private val simpleName = context.layout.inputObjectName(inputObject.name)

  override fun build(): CodegenJavaFile {
    return CodegenJavaFile(
        packageName = packageName,
        typeSpec = inputObject.typeSpec()
    )
  }

  override fun prepare() {
    context.resolver.registerSchemaType(
        inputObject.name,
        ClassName.get(packageName, simpleName)
    )
  }

  private fun IrInputObject.typeSpec() =
      TypeSpec
          .classBuilder(simpleName)
          .addModifiers(Modifier.PUBLIC)
          .applyIf(description?.isNotBlank() == true) { addJavadoc("$L\n", description!!) }
          .makeDataClassFromParameters(fields.map {
            it.toNamedType().toParameterSpec(context)
          })
          .addBuilder()
          .build()

  private fun TypeSpec.Builder.addBuilder(): TypeSpec.Builder {
    addMethod(Builder.builderFactoryMethod())

    val inputClassName = ClassName.get(packageName, simpleName)

    if (inputObject.fields.isEmpty()) {
      return addType(
          Builder(
              targetObjectClassName = inputClassName,
              fields = emptyList(),
              fieldDefaultValues = emptyMap(),
              fieldJavaDocs = emptyMap(),
              context = context
          ).build()
      )
    } else {
      val builderFields = inputObject.fields.map {
        context.layout.escapeReservedWord(it.name) to context.resolver.resolveIrType(it.type)
      }
      val builderFieldDefaultValues = inputObject.fields
        .filter {
          // ignore any custom type or object default values for now as we don't support them
          !it.type.isCustomScalar() && (it.type !is IrInputObjectType || it.defaultValue == null)
        }
        .associate { context.layout.escapeReservedWord(it.name) to it.defaultValue }
      val javaDocs = inputObject.fields
        .filter { !it.description.isNullOrBlank() }
        .associate { context.layout.escapeReservedWord(it.name) to it.description!! }
      return addType(
          Builder(
            targetObjectClassName = inputClassName,
            fields = builderFields,
            fieldDefaultValues = builderFieldDefaultValues,
            fieldJavaDocs = javaDocs,
            context = context
          ).build()
        )
    }
  }
}
