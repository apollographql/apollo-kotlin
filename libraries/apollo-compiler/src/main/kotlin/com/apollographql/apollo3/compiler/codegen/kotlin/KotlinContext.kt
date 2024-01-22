package com.apollographql.apollo3.compiler.codegen.kotlin

import com.apollographql.apollo3.compiler.GeneratedMethod
import com.apollographql.apollo3.compiler.TargetLanguage
import com.apollographql.apollo3.compiler.codegen.PropertyNameGenerator

internal interface KotlinContext: PropertyNameGenerator {
  val generateMethods: List<GeneratedMethod>
  val jsExport: Boolean
  val resolver: KotlinResolver
  val targetLanguageVersion: TargetLanguage
}

internal class KotlinSchemaContext(
    override val generateMethods: List<GeneratedMethod>,
    override val jsExport: Boolean,
    override val resolver: KotlinResolver,
    override val targetLanguageVersion: TargetLanguage,
    override val decapitalizeFields: Boolean,
    val layout: KotlinSchemaCodegenLayout,
): KotlinContext {
  fun isTargetLanguageVersionAtLeast(targetLanguage: TargetLanguage): Boolean {
    // Assumes TargetLanguage.KOTLIN_X_Y values are declared in increasing order
    return targetLanguageVersion.ordinal >= targetLanguage.ordinal
  }
}

internal class KotlinOperationsContext(
    override val generateMethods: List<GeneratedMethod>,
    override val jsExport: Boolean,
    override val resolver: KotlinResolver,
    override val targetLanguageVersion: TargetLanguage,
    override val decapitalizeFields: Boolean,
    val layout: KotlinOperationsCodegenLayout,
): KotlinContext


internal class KotlinResolverContext(
    override val generateMethods: List<GeneratedMethod>,
    override val jsExport: Boolean,
    override val resolver: KotlinResolver,
    override val targetLanguageVersion: TargetLanguage,
    override val decapitalizeFields: Boolean,
    val layout: KotlinResolverCodegenLayout,
): KotlinContext {
  fun isTargetLanguageVersionAtLeast(targetLanguage: TargetLanguage): Boolean {
    // Assumes TargetLanguage.KOTLIN_X_Y values are declared in increasing order
    return targetLanguageVersion.ordinal >= targetLanguage.ordinal
  }
}
