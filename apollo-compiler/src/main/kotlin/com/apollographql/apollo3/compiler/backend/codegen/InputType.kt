package com.apollographql.apollo3.compiler.backend.codegen

import com.apollographql.apollo3.api.InputObject
import com.apollographql.apollo3.compiler.applyIf
import com.apollographql.apollo3.compiler.backend.ast.CodeGenerationAst
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.TypeSpec


internal fun CodeGenerationAst.InputType.typeSpec() =
    TypeSpec
        .classBuilder(kotlinNameForInputObjectType(name))
        .applyIf(description.isNotBlank()) { addKdoc("%L\n", description) }
        .addAnnotation(suppressWarningsAnnotation)
        .makeDataClass(fields.map { it.toParameterSpec() })
        .addSuperinterface(InputObject::class)
        .build()

