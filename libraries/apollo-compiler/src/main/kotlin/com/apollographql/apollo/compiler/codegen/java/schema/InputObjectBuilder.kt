package com.apollographql.apollo.compiler.codegen.java.schema

import com.apollographql.apollo.compiler.JavaNullable
import com.apollographql.apollo.compiler.codegen.java.CodegenJavaFile
import com.apollographql.apollo.compiler.codegen.java.JavaClassBuilder
import com.apollographql.apollo.compiler.codegen.java.JavaClassNames
import com.apollographql.apollo.compiler.codegen.java.JavaSchemaContext
import com.apollographql.apollo.compiler.codegen.java.T
import com.apollographql.apollo.compiler.codegen.java.helpers.BuilderBuilder
import com.apollographql.apollo.compiler.codegen.java.helpers.NamedType
import com.apollographql.apollo.compiler.codegen.java.helpers.makeClassFromParameters
import com.apollographql.apollo.compiler.codegen.java.helpers.maybeAddDescription
import com.apollographql.apollo.compiler.codegen.java.helpers.toNamedType
import com.apollographql.apollo.compiler.codegen.java.helpers.toParameterSpec
import com.apollographql.apollo.compiler.codegen.java.javaPropertyName
import com.apollographql.apollo.compiler.codegen.typePackageName
import com.apollographql.apollo.compiler.codegen.typeUtilPackageName
import com.apollographql.apollo.compiler.ir.IrInputObject
import com.squareup.javapoet.ClassName
import com.squareup.javapoet.CodeBlock
import com.squareup.javapoet.FieldSpec
import com.squareup.javapoet.TypeSpec
import javax.lang.model.element.Modifier

internal class InputObjectBuilder(
    val context: JavaSchemaContext,
    val inputObject: IrInputObject,
) : JavaClassBuilder {
  private val packageName = context.layout.typePackageName()
  private val simpleName = context.layout.schemaTypeName(inputObject.name)

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

  private fun IrInputObject.typeSpec(): TypeSpec {
    val namedTypes = fields.map { it.toNamedType() }
    return TypeSpec
        .classBuilder(simpleName)
        .addModifiers(Modifier.PUBLIC)
        .maybeAddDescription(description)
        .makeClassFromParameters(
            context.generateMethods,
            namedTypes.map { it.toParameterSpec(context) },
            className = context.resolver.resolveSchemaType(inputObject.name)
        )
        .apply {
          if (isOneOf) {
            methodSpecs.replaceAll {
              if (it.isConstructor) {
                it.toBuilder().addCode(namedTypes.assertOneOfBlock(context)).build()
              } else {
                it
              }
            }
          }
        }
        .addBuilder()
        .build()
  }

  private fun TypeSpec.Builder.addBuilder(): TypeSpec.Builder {
    if (inputObject.fields.isEmpty()) {
      // The GraphQL spec doesn't allow an input with no fields
      return this
    } else {
      val builderFields = inputObject.fields.map {
        FieldSpec.builder(context.resolver.resolveIrType(it.type).withoutAnnotations(), context.layout.javaPropertyName(it.name))
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

private fun List<NamedType>.assertOneOfBlock(context: JavaSchemaContext): CodeBlock {
  val assertionsClassName: ClassName = if (context.nullableFieldStyle == JavaNullable.GUAVA_OPTIONAL) {
    // When using the Guava optionals, use the method generated in the project, as apollo-api doesn't depend on Guava
    ClassName.get(context.layout.typeUtilPackageName(), "Assertions")
  } else {
    JavaClassNames.Assertions
  }
  return CodeBlock.of("$T.assertOneOf(${joinToString { context.layout.javaPropertyName(it.graphQlName) }});\n", assertionsClassName)
}
