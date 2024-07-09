package com.apollographql.apollo.compiler.codegen.kotlin

import com.apollographql.apollo.compiler.GeneratedMethod
import com.apollographql.apollo.compiler.TargetLanguage
import com.apollographql.apollo.compiler.codegen.CommonLayout
import com.apollographql.apollo.compiler.codegen.OperationsLayout
import com.apollographql.apollo.compiler.codegen.SchemaLayout

internal interface KotlinContext {
  val layout: CommonLayout
  val generateMethods: List<GeneratedMethod>
  val jsExport: Boolean
  val resolver: KotlinResolver
  val targetLanguage: TargetLanguage

  fun isTargetLanguageVersionAtLeast(targetLanguage: TargetLanguage): Boolean {
    // Assumes TargetLanguage.KOTLIN_X_Y values are declared in increasing order
    return this.targetLanguage.ordinal >= targetLanguage.ordinal
  }
}
internal class KotlinSchemaContext(
    override val layout: SchemaLayout,
    override val generateMethods: List<GeneratedMethod>,
    override val jsExport: Boolean,
    override val resolver: KotlinResolver,
    override val targetLanguage: TargetLanguage,
): KotlinContext

internal class KotlinOperationsContext(
    override val layout: OperationsLayout,
    override val generateMethods: List<GeneratedMethod>,
    override val jsExport: Boolean,
    override val resolver: KotlinResolver,
    override val targetLanguage: TargetLanguage,
): KotlinContext
