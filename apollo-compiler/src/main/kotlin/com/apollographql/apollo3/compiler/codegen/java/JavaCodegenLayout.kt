package com.apollographql.apollo3.compiler.codegen.java

import com.apollographql.apollo3.compiler.PackageNameGenerator
import com.apollographql.apollo3.compiler.codegen.CodegenLayout
import com.apollographql.apollo3.compiler.escapeJavaReservedWord

class JavaCodegenLayout(
    packageNameGenerator: PackageNameGenerator,
    schemaPackageName: String,
    useSemanticNaming: Boolean,
) : CodegenLayout(packageNameGenerator, schemaPackageName, useSemanticNaming) {
  override fun escapeReservedWord(word: String): String = word.escapeJavaReservedWord()
}
