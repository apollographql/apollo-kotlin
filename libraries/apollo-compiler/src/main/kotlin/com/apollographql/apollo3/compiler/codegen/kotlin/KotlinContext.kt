package com.apollographql.apollo3.compiler.codegen.kotlin

import com.apollographql.apollo3.compiler.GeneratedMethod
import com.apollographql.apollo3.compiler.TargetLanguage
import com.apollographql.apollo3.compiler.codegen.SchemaAndOperationsLayoutImpl

internal class KotlinContext(
    val generateMethods: List<GeneratedMethod>,
    val jsExport: Boolean,
    val layout: SchemaAndOperationsLayoutImpl,
    val resolver: KotlinResolver,
    val targetLanguage: TargetLanguage,
) {
  fun isTargetLanguageVersionAtLeast(targetLanguage: TargetLanguage): Boolean {
    // Assumes TargetLanguage.KOTLIN_X_Y values are declared in increasing order
    return this.targetLanguage.ordinal >= targetLanguage.ordinal
  }
}
