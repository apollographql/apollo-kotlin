package com.apollographql.apollo3.compiler.codegen.java.file

import com.apollographql.apollo3.compiler.applyIf
import com.apollographql.apollo3.compiler.codegen.java.CodegenJavaFile
import com.apollographql.apollo3.compiler.codegen.java.JavaClassBuilder
import com.apollographql.apollo3.compiler.codegen.java.JavaContext
import com.apollographql.apollo3.compiler.codegen.java.L
import com.apollographql.apollo3.compiler.codegen.java.helpers.BuilderBuilder
import com.apollographql.apollo3.compiler.codegen.java.helpers.makeDataClassFromParameters
import com.apollographql.apollo3.compiler.codegen.java.helpers.maybeAddDescription
import com.apollographql.apollo3.compiler.codegen.java.helpers.toNamedType
import com.apollographql.apollo3.compiler.codegen.java.helpers.toParameterSpec
import com.apollographql.apollo3.compiler.ir.IrInputObject
import com.squareup.javapoet.ClassName
import com.squareup.javapoet.FieldSpec
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
          .maybeAddDescription(description)
          .makeDataClassFromParameters(fields.map {
            it.toNamedType().toParameterSpec(context)
          })
          .addBuilder()
          .build()

  private fun TypeSpec.Builder.addBuilder(): TypeSpec.Builder {
    if (inputObject.fields.isEmpty()) {
      // The GraphQL spec doesn't allow an input with no fields
      return this
    } else {
      val builderFields = inputObject.fields.map {
        FieldSpec.builder(context.resolver.resolveIrType(it.type).withoutAnnotations(), context.layout.propertyName(it.name))
            .maybeAddDescription(it.description)
            .build()
      }
      return addMethod(BuilderBuilder.builderFactoryMethod())
          .addType(
              BuilderBuilder(
                  targetObjectClassName = ClassName.get(packageName, simpleName),
                  fields = builderFields,
                  context = context
              ).build()
          )
    }
  }
}
