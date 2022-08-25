package com.apollographql.apollo3.compiler.codegen.java

import com.apollographql.apollo3.compiler.PackageNameGenerator
import com.apollographql.apollo3.compiler.codegen.CodegenLayout
import com.apollographql.apollo3.compiler.escapeJavaReservedWord
import com.apollographql.apollo3.compiler.escapeTypeReservedWord
import com.apollographql.apollo3.compiler.ir.Ir

internal class JavaCodegenLayout(
    ir: Ir,
    packageNameGenerator: PackageNameGenerator,
    schemaPackageName: String,
    useSemanticNaming: Boolean,
    useSchemaPackageNameForFragments: Boolean,
) : CodegenLayout(
    ir,
    packageNameGenerator,
    schemaPackageName,
    useSemanticNaming,
    useSchemaPackageNameForFragments,
) {
  override fun escapeReservedWord(word: String): String = word.escapeJavaReservedWord()

  // We used to write upper case enum values but the server can define different values with different cases
  // See https://github.com/apollographql/apollo-android/issues/3035
  internal fun enumValueName(name: String) = name.escapeTypeReservedWord() ?: regularIdentifier(name)

  fun builderPackageName(): String = "${typePackageName()}.builder"
}
