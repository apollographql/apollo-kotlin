package com.apollographql.apollo3.compiler.codegen.kotlin

import com.apollographql.apollo3.compiler.TargetLanguage

class KotlinContext(
    val layout: KotlinCodegenLayout,
    val resolver: KotlinResolver,
    val targetLanguageVersion: TargetLanguage,
) {
  fun isTargetLanguageVersionAtLeast(targetLanguage: TargetLanguage): Boolean {
    // Assumes TargetLanguage.KOTLIN_X_Y values are declared in increasing order
    return targetLanguageVersion.ordinal >= targetLanguage.ordinal
  }
}
