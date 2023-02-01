package com.apollographql.apollo3.compiler.codegen.kotlin

import com.apollographql.apollo3.compiler.PackageNameGenerator
import com.apollographql.apollo3.compiler.codegen.CodegenLayout
import com.apollographql.apollo3.compiler.escapeKotlinReservedWord
import com.apollographql.apollo3.compiler.escapeKotlinReservedWordInEnum
import com.apollographql.apollo3.compiler.escapeKotlinReservedWordInSealedClass
import com.apollographql.apollo3.compiler.ir.IrOperations
import com.apollographql.apollo3.compiler.ir.IrSchemaType

internal class KotlinCodegenLayout(
    allTypes: List<IrSchemaType>,
    packageNameGenerator: PackageNameGenerator,
    schemaPackageName: String,
    useSemanticNaming: Boolean,
    decapitalizeFields: Boolean,
) : CodegenLayout(
    allTypes,
    packageNameGenerator,
    schemaPackageName,
    useSemanticNaming,
    decapitalizeFields,
) {
  override fun escapeReservedWord(word: String): String = word.escapeKotlinReservedWord()

  /**
   * Enum value name to use when generating enums as sealed classes
   */
  internal fun enumAsSealedClassValueName(name: String) = name.escapeKotlinReservedWordInSealedClass()

  /**
   * Enum value name to use when generating enums as enums
   */
  internal fun enumAsEnumValueName(name: String) = name.escapeKotlinReservedWordInEnum()
}
