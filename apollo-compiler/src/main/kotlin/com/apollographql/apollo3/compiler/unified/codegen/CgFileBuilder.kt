package com.apollographql.apollo3.compiler.unified.codegen

import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.TypeSpec

/**
 * KotlinPoet [FileSpec] are non qualified. This is a simple wrapper that carries a package name so that we can write the file
 *
 * If multiple [TypeSpec] are in the same [packageName], [fileName] is mandatory
 */
class CgFile(
    val packageName: String,
    val typeSpecs: List<TypeSpec>,
    val fileName: String,
) {
  constructor(packageName: String, typeSpec: TypeSpec) : this(packageName, listOf(typeSpec), typeSpec.name!!)
}

interface CgFileBuilder {
  fun prepare()
  fun build(): CgFile
}