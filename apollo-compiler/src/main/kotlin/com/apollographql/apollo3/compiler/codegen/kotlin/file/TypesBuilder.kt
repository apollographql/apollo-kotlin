package com.apollographql.apollo3.compiler.codegen.kotlin.file

import com.apollographql.apollo3.api.CompiledNamedType
import com.apollographql.apollo3.api.CompiledType
import com.apollographql.apollo3.api.CustomScalarType
import com.apollographql.apollo3.api.EnumType
import com.apollographql.apollo3.api.InterfaceType
import com.apollographql.apollo3.api.ObjectType
import com.apollographql.apollo3.api.UnionType
import com.apollographql.apollo3.compiler.codegen.Identifier
import com.apollographql.apollo3.compiler.codegen.Identifier.type
import com.apollographql.apollo3.compiler.codegen.kotlin.KotlinContext
import com.apollographql.apollo3.compiler.codegen.kotlin.CgFile
import com.apollographql.apollo3.compiler.codegen.kotlin.CgFileBuilder
import com.apollographql.apollo3.compiler.codegen.kotlin.KotlinResolver
import com.apollographql.apollo3.compiler.codegen.kotlin.helpers.maybeAddDeprecation
import com.apollographql.apollo3.compiler.codegen.kotlin.helpers.maybeAddDescription
import com.apollographql.apollo3.compiler.ir.IrCustomScalar
import com.apollographql.apollo3.compiler.ir.IrEnum
import com.apollographql.apollo3.compiler.ir.IrInterface
import com.apollographql.apollo3.compiler.ir.IrObject
import com.apollographql.apollo3.compiler.ir.IrUnion
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.asTypeName
import com.squareup.kotlinpoet.joinToCode

internal fun IrCustomScalar.typePropertySpec(): PropertySpec {
  /**
   * Custom Scalars without a mapping will generate code using [AnyResponseAdapter] directly
   * so the fallback isn't really required here. We still write it as a way to hint the user
   * to what's happening behind the scenes
   */
  val kotlinName = kotlinName ?: "kotlin.Any"
  return PropertySpec
      .builder(Identifier.type, CustomScalarType::class)
      .initializer("%T(%S, %S)", CustomScalarType::class.asTypeName(), name, kotlinName)
      .build()
}

internal fun IrEnum.typePropertySpec(): PropertySpec {
  return PropertySpec
      .builder(Identifier.type, EnumType::class)
      .initializer("%T(%S)", EnumType::class.asTypeName(), name)
      .build()
}

private fun Set<String>.toCode(): CodeBlock {
  val builder = CodeBlock.builder()
  builder.add("listOf(")
  builder.add("%L", sorted().map { CodeBlock.of("%S", it) }.joinToCode(", "))
  builder.add(")")
  return builder.build()
}

private fun List<String>.implementsToCode(resolver: KotlinResolver): CodeBlock {
  val builder = CodeBlock.builder()
  builder.add("listOf(")
  builder.add("%L", sorted().map {
    resolver.resolveCompiledType(it)
  }.joinToCode(", "))
  builder.add(")")
  return builder.build()
}

internal fun IrObject.typePropertySpec(resolver: KotlinResolver): PropertySpec {
  val builder = CodeBlock.builder()
  builder.add("%T(name = %S", ObjectType::class.asTypeName(), name)
  if (keyFields.isNotEmpty()) {
    builder.add(", ")
    builder.add("keyFields = %L", keyFields.toCode())
  }
  if (implements.isNotEmpty()) {
    builder.add(", ")
    builder.add("implements = %L", implements.implementsToCode(resolver))
  }
  builder.add(")")

  return PropertySpec
      .builder(type, ObjectType::class)
      .initializer(builder.build())
      .build()
}

internal fun IrInterface.typePropertySpec(resolver: KotlinResolver): PropertySpec {
  val builder = CodeBlock.builder()
  builder.add("%T(name = %S", InterfaceType::class.asTypeName(), name)
  if (keyFields.isNotEmpty()) {
    builder.add(", ")
    builder.add("keyFields = %L", keyFields.toCode())
  }
  if (implements.isNotEmpty()) {
    builder.add(", ")
    builder.add("implements = %L", implements.implementsToCode(resolver))
  }
  builder.add(")")

  return PropertySpec
      .builder(type, InterfaceType::class)
      .initializer(builder.build())
      .build()
}


internal fun IrUnion.typePropertySpec(resolver: KotlinResolver): PropertySpec {
  val builder = CodeBlock.builder()
  builder.add(members.map {
    resolver.resolveCompiledType(it)
  }.joinToCode(", "))

  return PropertySpec
      .builder(type, UnionType::class)
      .maybeAddDescription(description)
      .maybeAddDeprecation(deprecationReason)
      .initializer("%T(%S, %L)", UnionType::class.asTypeName(), name, builder.build())
      .build()
}