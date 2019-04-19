package com.apollographql.apollo.compiler.codegen.kotlin

import com.apollographql.apollo.api.ScalarType
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy

fun KotlinCodeGen.customScalarTypeSpec(customTypes: Map<String, String>): TypeSpec {
  return TypeSpec.enumBuilder("CustomType")
      .addAnnotation(generatedByApolloAnnotation)
      .addSuperinterface(ScalarType::class.java)
      .apply {
        customTypes.map { (schemaType, customType) ->
          addEnumConstant(
              name = schemaType.normalizeGraphQLType().toUpperCase(),
              typeSpec = enumConstantTypeSpec(schemaType = schemaType, customType = customType)
          )
        }
      }
      .build()
}

private fun enumConstantTypeSpec(schemaType: String, customType: String): TypeSpec {
  return TypeSpec.anonymousClassBuilder()
      .addFunction(FunSpec.builder("typeName")
          .addModifiers(KModifier.OVERRIDE)
          .returns(String::class)
          .addStatement("return %S", schemaType)
          .build()
      )
      .addFunction(FunSpec.builder("javaType")
          .returns(Class::class.asClassName().parameterizedBy(STAR))
          .addModifiers(KModifier.OVERRIDE)
          .addStatement("return %L::class.java", customType)
          .build()
      )
      .build()
}