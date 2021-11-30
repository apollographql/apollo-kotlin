package com.apollographql.apollo3.compiler.codegen.kotlin

import com.apollographql.apollo3.compiler.PackageNameGenerator
import com.apollographql.apollo3.compiler.codegen.CodegenLayout
import com.apollographql.apollo3.compiler.escapeKotlinReservedEnumValueNames
import com.apollographql.apollo3.compiler.escapeKotlinReservedWord

class KotlinCodegenLayout(
    packageNameGenerator: PackageNameGenerator,
    schemaPackageName: String,
    useSemanticNaming: Boolean,
) : CodegenLayout(packageNameGenerator, schemaPackageName, useSemanticNaming) {

  override fun escapeReservedWord(word: String): String = word.escapeKotlinReservedWord()

  internal fun sealedClassValueName(name: String) = name.escapeKotlinReservedEnumValueNames()
}
