package com.apollographql.apollo3.compiler.codegen.kotlin

import com.apollographql.apollo3.compiler.CodegenType
import com.apollographql.apollo3.compiler.PackageNameGenerator
import com.apollographql.apollo3.compiler.codegen.OperationsCodegenLayout
import com.apollographql.apollo3.compiler.codegen.ResolverCodegenLayout
import com.apollographql.apollo3.compiler.codegen.SchemaCodegenLayout
import com.apollographql.apollo3.compiler.internal.escapeKotlinReservedWord
import com.apollographql.apollo3.compiler.internal.escapeKotlinReservedWordInEnum
import com.apollographql.apollo3.compiler.internal.escapeKotlinReservedWordInSealedClass

internal class KotlinSchemaCodegenLayout(
    allTypes: List<CodegenType>,
    schemaPackageName: String,
) : SchemaCodegenLayout(
    allTypes,
    schemaPackageName,
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

internal class KotlinOperationsCodegenLayout(
    allTypes: List<CodegenType>,
    packageNameGenerator: PackageNameGenerator,
    useSemanticNaming: Boolean,
) : OperationsCodegenLayout(
    allTypes = allTypes,
    packageNameGenerator = packageNameGenerator,
    useSemanticNaming = useSemanticNaming,
) {
  override fun escapeReservedWord(word: String): String = word.escapeKotlinReservedWord()
}

internal class KotlinResolverCodegenLayout(
    allTypes: List<CodegenType>,
    packageName: String,
) : ResolverCodegenLayout(
    allTypes = allTypes,
    packageName = packageName,
) {
  override fun escapeReservedWord(word: String): String = word.escapeKotlinReservedWord()
}