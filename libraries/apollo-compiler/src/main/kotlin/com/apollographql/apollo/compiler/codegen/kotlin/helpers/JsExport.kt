package com.apollographql.apollo.compiler.codegen.kotlin.helpers

import com.apollographql.apollo.compiler.internal.applyIf
import com.apollographql.apollo.compiler.codegen.kotlin.KotlinContext
import com.apollographql.apollo.compiler.codegen.kotlin.KotlinSymbols
import com.apollographql.apollo.compiler.codegen.maybeFlatten
import com.apollographql.apollo.compiler.ir.IrModel
import com.apollographql.apollo.compiler.ir.IrModelGroup
import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.TypeSpec

internal val suppressNonExportableType = AnnotationSpec.builder(KotlinSymbols.Suppress)
  .addMember("%S", "NON_EXPORTABLE_TYPE")
  .build()

internal fun TypeSpec.Builder.maybeAddJsExport(context: KotlinContext): TypeSpec.Builder {
  return applyIf(context.jsExport) {
    addKdoc("""
      Note that we add [JsExport] to the top-level operation because it cannot be applied to the
      nested classes within. [JsExport] is only applicable to top-level objects. We need the data
      classes so that they are not mangled during compilation and we can safely cast parsed JSON.
    """.trimIndent())
    addAnnotation(KotlinSymbols.JsExport)
    addAnnotation(suppressNonExportableType)
  }
}

/**
 * Fragments need to be flattened at depth 1 to avoid having all classes polluting the fragment's package name
 * For JsExports we cannot have nested interfaces, so we fully flatten, and then prefix with the main model name and __
 */
internal fun IrModelGroup.flattenFragmentModels(flatten: Boolean, context: KotlinContext, mainModelName: String): List<IrModel> {
  val flattenOverride = flatten || context.jsExport
  return maybeFlatten(flattenOverride, if (context.jsExport) 0 else 1).flatMap { it.models }.jsExportNamePrefix(context, mainModelName)
}

/**
 * JsExport does not allow nested interfaces, so we have to hoist generated fragment interfaces up to the main package. To avoid
 * name conflicts, we prefix with the main model name.
 *
 * @param context
 * @param mainModelName
 * @return
 */
private fun List<IrModel>.jsExportNamePrefix(context: KotlinContext, mainModelName: String): List<IrModel> {
  return map { if (it.modelName != mainModelName && context.jsExport) it.copy(modelName = "${mainModelName}__${it.modelName}") else it }
}
