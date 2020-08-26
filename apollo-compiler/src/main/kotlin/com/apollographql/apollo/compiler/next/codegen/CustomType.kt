package com.apollographql.apollo.compiler.next.codegen

import com.apollographql.apollo.api.ScalarType
import com.apollographql.apollo.compiler.applyIf
import com.apollographql.apollo.compiler.ast.CustomTypes
import com.apollographql.apollo.compiler.codegen.kotlin.KotlinCodeGen.normalizeGraphQLType
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy

internal fun CustomTypes.typeSpec(generateAsInternal: Boolean = false) =
    TypeSpec
        .enumBuilder("CustomType")
        .applyIf(generateAsInternal) { addModifiers(KModifier.INTERNAL) }
        .addSuperinterface(ScalarType::class.java)
        .apply {
          toSortedMap()
              .map { (schemaType, customType) ->
                addEnumConstant(
                    name = schemaType.normalizeGraphQLType().toUpperCase(),
                    typeSpec = enumConstantTypeSpec(schemaType = schemaType, customType = customType)
                )
              }
        }
        .build()

private fun enumConstantTypeSpec(schemaType: String, customType: String) =
    TypeSpec
        .anonymousClassBuilder()
        .addFunction(FunSpec.builder("typeName")
            .addModifiers(KModifier.OVERRIDE)
            .returns(String::class)
            .addStatement("return %S", schemaType)
            .build()
        )
        .addFunction(FunSpec.builder("className")
            .returns(String::class)
            .addModifiers(KModifier.OVERRIDE)
            .addStatement("return %S", customType)
            .build()
        )
        .build()
