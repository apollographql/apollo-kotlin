package com.apollographql.apollo3.compiler.codegen.kotlin.file

import com.apollographql.apollo3.compiler.codegen.Identifier
import com.apollographql.apollo3.compiler.codegen.Identifier.Builder
import com.apollographql.apollo3.compiler.codegen.kotlin.CgFile
import com.apollographql.apollo3.compiler.codegen.kotlin.CgFileBuilder
import com.apollographql.apollo3.compiler.codegen.kotlin.KotlinContext
import com.apollographql.apollo3.compiler.codegen.kotlin.helpers.NamedType
import com.apollographql.apollo3.compiler.codegen.kotlin.helpers.makeClassFromParameters
import com.apollographql.apollo3.compiler.codegen.kotlin.helpers.maybeAddDescription
import com.apollographql.apollo3.compiler.codegen.kotlin.helpers.toNamedType
import com.apollographql.apollo3.compiler.codegen.kotlin.helpers.toParameterSpec
import com.apollographql.apollo3.compiler.codegen.kotlin.helpers.toPropertySpec
import com.apollographql.apollo3.compiler.codegen.kotlin.helpers.toSetterFunSpec
import com.apollographql.apollo3.compiler.ir.IrInputObject
import com.apollographql.apollo3.compiler.ir.IrNonNullType
import com.apollographql.apollo3.compiler.ir.IrOptionalType
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.TypeSpec

internal class InputObjectBuilder(
    val context: KotlinContext,
    val inputObject: IrInputObject,
    val generateInputBuilders: Boolean,
    val withDefaultArguments: Boolean
) : CgFileBuilder {
  private val packageName = context.layout.typePackageName()
  private val simpleName = context.layout.inputObjectName(inputObject.name)
  private val className = ClassName(packageName, simpleName)

  override fun build(): CgFile {
    return CgFile(
        packageName = packageName,
        fileName = simpleName,
        typeSpecs = listOf(inputObject.typeSpec())
    )
  }

  override fun prepare() {
    context.resolver.registerSchemaType(
        inputObject.name,
        className
    )
  }

  private fun IrInputObject.typeSpec(): TypeSpec {
    val namedTypes = fields.map {
      it.toNamedType()
    }
    return TypeSpec
        .classBuilder(simpleName)
        .maybeAddDescription(description)
        .makeClassFromParameters(
            context.generateMethods,
            namedTypes.map { it.toParameterSpec(context, withDefaultArguments) },
            className = context.resolver.resolveSchemaType(inputObject.name)
        )
        .apply {
          if (namedTypes.isNotEmpty() && generateInputBuilders) {
            addType(namedTypes.builderTypeSpec(context, className))
          }
        }
        .build()
  }
}


internal fun List<NamedType>.builderTypeSpec(context: KotlinContext, returnedClassName: ClassName): TypeSpec {
  return TypeSpec.classBuilder(Builder)
      .apply {
        forEach {
          addProperty(it.toPropertySpec(context))
          addFunction(it.toSetterFunSpec(context))
        }
      }
      .addFunction(toBuildFunSpec(context, returnedClassName))
      .build()
}

private fun List<NamedType>.toBuildFunSpec(context: KotlinContext, returnedClassName: ClassName): FunSpec {
  return FunSpec.builder(Identifier.build)
      .returns(returnedClassName)
      .addCode(
          CodeBlock.builder()
              .add("return·%T(\n", returnedClassName)
              .indent()
              .apply {
                forEach {
                  val propertyName = context.layout.propertyName(it.graphQlName)
                  add("%L·=·%L", propertyName, propertyName)
                  if (it.type is IrNonNullType && it.type.ofType !is IrOptionalType) {
                    add("·?:·error(\"missing·value·for·$propertyName\")")
                  }
                  add(",\n")
                }
              }
              .unindent()
              .add(")")
              .build()
      )
      .build()
}