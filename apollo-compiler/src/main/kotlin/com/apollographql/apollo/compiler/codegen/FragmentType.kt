package com.apollographql.apollo.compiler.codegen

import com.apollographql.apollo.api.GraphqlFragment
import com.apollographql.apollo.api.internal.ResponseReader
import com.apollographql.apollo.compiler.applyIf
import com.apollographql.apollo.compiler.ast.CodeGenerationAst
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.joinToCode

internal fun CodeGenerationAst.FragmentType.typeSpec(generateAsInternal: Boolean): TypeSpec {
  val fragmentType = checkNotNull(nestedTypes[rootType]) {
    "Failed to resolve operation root data type"
  }
  val nestedTypeSpecs = nestedTypes.minus(rootType).values.map { type -> type.typeSpec() }
  val defaultImplementationTypeSpec = nestedTypeSpecs.find { typeSpec -> typeSpec.name == defaultImplementation.name }!!
  return TypeSpec
      .interfaceBuilder(fragmentType.name)
      .addAnnotation(suppressWarningsAnnotation)
      .applyIf(generateAsInternal) { addModifiers(KModifier.INTERNAL) }
      .addSuperinterface(GraphqlFragment::class)
      .applyIf(fragmentType.description.isNotBlank()) { addKdoc("%L\n", fragmentType.description) }
      .addProperties(fragmentType.fields.map { field -> field.asPropertySpec() })
      .addTypes(nestedTypeSpecs)
      .addType(TypeSpec.companionObjectBuilder()
          .addProperty(PropertySpec.builder("FRAGMENT_DEFINITION", String::class)
              .initializer(CodeBlock.of("%S", fragmentDefinition))
              .build()
          )
          .addFunction(
              FunSpec.builder("invoke")
                  .addModifiers(KModifier.OPERATOR)
                  .returns(ClassName.bestGuess(fragmentType.name))
                  .addParameters(defaultImplementationTypeSpec.propertySpecs.map { property -> property.asParameter() })
                  .addCode(CodeBlock.builder()
                      .addStatement("return·%T(", defaultImplementation.asTypeName())
                      .indent()
                      .add(
                          defaultImplementationTypeSpec.propertySpecs
                              .map { property -> CodeBlock.of("%L = %L", property.name, property.name) }
                              .joinToCode(separator = ",\n", suffix = "\n")
                      )
                      .unindent()
                      .addStatement(")")
                      .build()
                  )
                  .build()
          )
          .addFunction(
              FunSpec.builder("invoke")
                  .addModifiers(KModifier.OPERATOR)
                  .returns(ClassName.bestGuess(fragmentType.name))
                  .addParameter(ParameterSpec.builder("reader", ResponseReader::class).build())
                  .addStatement("return %T(reader)", defaultImplementation.asTypeName())
                  .build()
          )
          .build()
      )
      .build()
}
