package com.apollographql.apollo3.compiler.codegen.kotlin

import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec

/**
 * A file that is going to be converted to a KotlinPoet [FileSpec]
 */
internal class CgFile(
    val packageName: String,
    val typeSpecs: List<TypeSpec> = emptyList(),
    val propertySpecs: List<PropertySpec> = emptyList(),
    val funSpecs: List<FunSpec> = emptyList(),
    val fileName: String,
)

internal interface CgFileBuilder {
  fun prepare()
  fun build(): CgFile
}

