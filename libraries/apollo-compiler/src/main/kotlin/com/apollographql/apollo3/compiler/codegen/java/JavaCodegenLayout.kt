package com.apollographql.apollo3.compiler.codegen.java

import com.apollographql.apollo3.compiler.CodegenType
import com.apollographql.apollo3.compiler.PackageNameGenerator
import com.apollographql.apollo3.compiler.codegen.OperationsCodegenLayout
import com.apollographql.apollo3.compiler.codegen.SchemaCodegenLayout
import com.apollographql.apollo3.compiler.internal.escapeJavaReservedWord
import com.apollographql.apollo3.compiler.internal.escapeTypeReservedWord

internal class JavaSchemaCodegenLayout(
    allTypes: List<CodegenType>,
    schemaPackageName: String,
) : SchemaCodegenLayout(
    allTypes,
    schemaPackageName,
) {
  override fun escapeReservedWord(word: String): String = word.escapeJavaReservedWord()

  // We used to write upper case enum values but the server can define different values with different cases
  // See https://github.com/apollographql/apollo-android/issues/3035
  internal fun enumValueName(name: String) = name.escapeTypeReservedWord() ?: regularIdentifier(name)

  fun builderPackageName(): String = "${typePackageName()}.builder"

  fun utilPackageName() = "$schemaPackageName.util"
}

internal class JavaOperationsCodegenLayout(
    allTypes: List<CodegenType>,
    packageNameGenerator: PackageNameGenerator,
    useSemanticNaming: Boolean,
) : OperationsCodegenLayout(
    allTypes,
    packageNameGenerator,
    useSemanticNaming,
) {
  override fun escapeReservedWord(word: String): String = word.escapeJavaReservedWord()

  // We used to write upper case enum values but the server can define different values with different cases
  // See https://github.com/apollographql/apollo-android/issues/3035
  internal fun enumValueName(name: String) = name.escapeTypeReservedWord() ?: regularIdentifier(name)
}
