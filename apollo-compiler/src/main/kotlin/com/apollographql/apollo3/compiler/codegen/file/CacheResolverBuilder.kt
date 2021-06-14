package com.apollographql.apollo3.compiler.codegen.file

import com.apollographql.apollo3.api.CompiledField
import com.apollographql.apollo3.api.Executable
import com.apollographql.apollo3.ast.GQLObjectTypeDefinition
import com.apollographql.apollo3.ast.Schema
import com.apollographql.apollo3.cache.normalized.CacheResolver
import com.apollographql.apollo3.compiler.codegen.CgContext
import com.apollographql.apollo3.compiler.codegen.CgFile
import com.apollographql.apollo3.compiler.codegen.CgFileBuilder
import com.apollographql.apollo3.compiler.codegen.Identifier.cacheKeyForObject
import com.apollographql.apollo3.compiler.codegen.Identifier.field
import com.apollographql.apollo3.compiler.codegen.Identifier.variables
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.asTypeName
import com.squareup.kotlinpoet.joinToCode

class CacheResolverBuilder(
    private val context: CgContext,
    private val schema: Schema
) : CgFileBuilder {
  private val layout = context.layout
  private val packageName = layout.typePackageName()

  private val mappings =  schema.typeDefinitions.values.filterIsInstance<GQLObjectTypeDefinition>().mapNotNull {
    val keyFields = schema.keyFields(it.name)
    if (keyFields.isNotEmpty()) {
      it.name to keyFields
    } else {
      null
    }
  }

  fun isEmpty(): Boolean = mappings.isEmpty()

  override fun prepare() {}

  override fun build(): CgFile {
    return CgFile(
        packageName,
        typeSpec()
    )
  }

  private fun typeSpec(): TypeSpec {
    return TypeSpec.classBuilder(layout.cacheResolverName())
        .addModifiers(KModifier.OPEN)
        .superclass(CacheResolver::class)
        .addFunction(cacheKeyForObjectFunSpec())
        .build()
  }

  private fun cacheKeyForObjectFunSpec(): FunSpec {
    val builder = CodeBlock.builder()

    builder.beginControlFlow("return when(map[\"__typename\"])")
    mappings.forEach {
      builder.add("%S -> cacheKey(map, %L)\n", it.first, it.second.map { CodeBlock.of("%S", it) }.joinToCode(","))
    }

    builder.add("else -> null\n")
    builder.endControlFlow()

    return FunSpec.builder(cacheKeyForObject)
        .addModifiers(KModifier.OVERRIDE, KModifier.OPEN)
        .addParameter(field, CompiledField::class.java.asTypeName())
        .addParameter(variables, Executable.Variables::class.java.asTypeName())
        .addParameter("map", Map::class.asClassName().parameterizedBy(String::class.asClassName(), Any::class.asClassName().copy(nullable = true)))
        .addCode(builder.build())
        .build()
  }
}
