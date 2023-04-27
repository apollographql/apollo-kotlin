package com.apollographql.apollo3.compiler.codegen.kotlin.helpers

import com.apollographql.apollo3.compiler.applyIf
import com.apollographql.apollo3.compiler.codegen.kotlin.KotlinContext
import com.apollographql.apollo3.compiler.codegen.kotlin.KotlinSymbols
import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.TypeSpec

internal val suppressNonExportableType = AnnotationSpec.builder(KotlinSymbols.Suppress)
  .addMember("%S", "NON_EXPORTABLE_TYPE")
  .build()

internal fun TypeSpec.Builder.maybeAddJsExport(context: KotlinContext, suppressNonExportable: Boolean = false): TypeSpec.Builder {
  return applyIf(context.jsExport) {
    addAnnotation(KotlinSymbols.JsExport)
    if (suppressNonExportable) {
      addAnnotation(suppressNonExportableType)
    }
  }
}
