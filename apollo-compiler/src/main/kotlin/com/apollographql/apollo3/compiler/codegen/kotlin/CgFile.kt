package com.apollographql.apollo3.compiler.codegen.kotlin

import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.TypeSpec

/**
 * A file that is going to be converted to a KotlinPoet [FileSpec]
 */
class CgFile(
    val packageName: String,
    val typeSpecs: List<TypeSpec> = emptyList(),
    val fileName: String,
    val isTest: Boolean = false
)

interface CgFileBuilder {
  fun prepare()
  fun build(): CgFile
}

