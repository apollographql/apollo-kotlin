package com.apollographql.apollo3.compiler.codegen.kotlin

import com.apollographql.apollo3.compiler.PackageNameGenerator
import com.apollographql.apollo3.compiler.codegen.CodegenLayout
import com.apollographql.apollo3.compiler.escapeKotlinReservedEnumValueNames
import com.apollographql.apollo3.compiler.escapeKotlinReservedWord

class KotlinCodegenLayout(
    packageNameGenerator: PackageNameGenerator,
    schemaPackageName: String,
    useSemanticNaming: Boolean,
    useSchemaPackageNameForFragments: Boolean,
    typePackageName: String,
) : CodegenLayout(
    packageNameGenerator,
    schemaPackageName,
    useSemanticNaming,
    useSchemaPackageNameForFragments,
    typePackageName
) {

  override fun escapeReservedWord(word: String): String = word.escapeKotlinReservedWord()

  /**
   * Enum value name to use when generating enums as sealed classes
   */
  internal fun enumAsSealedClassValueName(name: String) = regularIdentifier(name)

  /**
   * Enum value name to use when generating enums as enums
   */
  internal fun enumAsEnumValueName(name: String) = name.escapeKotlinReservedEnumValueNames()
}
