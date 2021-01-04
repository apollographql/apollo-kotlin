package com.apollographql.apollo.compiler.backend.codegen

import com.apollographql.apollo.api.Adaptable
import com.apollographql.apollo.api.GraphqlFragment
import com.apollographql.apollo.api.internal.ResponseAdapter
import com.apollographql.apollo.api.internal.ResponseReader
import com.apollographql.apollo.compiler.applyIf
import com.apollographql.apollo.compiler.backend.ast.CodeGenerationAst
import com.apollographql.apollo.compiler.escapeKotlinReservedWord
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.asTypeName

internal fun CodeGenerationAst.FragmentType.interfaceTypeSpec(generateAsInternal: Boolean): TypeSpec {
  return TypeSpec
      .interfaceBuilder(this.name.escapeKotlinReservedWord())
      .addAnnotation(suppressWarningsAnnotation)
      .applyIf(generateAsInternal) { addModifiers(KModifier.INTERNAL) }
      .addSuperinterface(GraphqlFragment::class)
      .applyIf(this.description.isNotBlank()) { addKdoc("%L\n", this@interfaceTypeSpec.description) }
      .addProperties(this.interfaceType.fields.map { field -> field.asPropertySpec() })
      .addTypes(
          this.interfaceType.nestedObjects.map { nestedObject ->
            nestedObject.typeSpec()
          }
      )
      .addType(
          TypeSpec.companionObjectBuilder()
              .addProperty(PropertySpec.builder("FRAGMENT_DEFINITION", String::class)
                  .initializer(CodeBlock.of("%S", fragmentDefinition))
                  .build()
              ).addFunction(
                  FunSpec.builder("invoke")
                      .addModifiers(KModifier.OPERATOR)
                      .returns(this.interfaceType.typeRef.asTypeName())
                      .addParameter(ParameterSpec.builder("reader", ResponseReader::class).build())
                      .addStatement(
                          "return·%T.fromResponse(reader)",
                          this.defaultImplementationType.typeRef.asAdapterTypeName(),
                      )
                      .build()
              )
              .addFunctions(
                  this@interfaceTypeSpec.interfaceType.fragmentAccessors.map { accessor ->
                    FunSpec
                        .builder(accessor.name.escapeKotlinReservedWord())
                        .receiver(this.typeRef.asTypeName())
                        .returns(accessor.typeRef.asTypeName().copy(nullable = true))
                        .addStatement("return this as? %T", accessor.typeRef.asTypeName())
                        .build()

                  }
              )
              .build()
      )
      .build()
}


internal fun CodeGenerationAst.FragmentType.implementationTypeSpec(generateAsInternal: Boolean): TypeSpec {
  return this.defaultImplementationType
      .typeSpec()
      .toBuilder()
      .addSuperinterface(GraphqlFragment::class)
      .addSuperinterface(Adaptable::class.asTypeName().parameterizedBy(defaultImplementationType.typeRef.asTypeName()))
      .addFunction(
          FunSpec.builder("adapter")
              .addModifiers(KModifier.OVERRIDE)
              .returns(defaultImplementationType.typeRef.asAdapterTypeName())
              .addCode("return·%T", this.defaultImplementationType.typeRef.asAdapterTypeName())
              .build()
      )
      .applyIf(generateAsInternal) { addModifiers(KModifier.INTERNAL) }
      .build()
}
