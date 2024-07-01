package com.apollographql.apollo.compiler.codegen.kotlin.operations.util

import com.apollographql.apollo.compiler.codegen.kotlin.KotlinContext
import com.apollographql.apollo.compiler.ir.IrModelGroup
import com.squareup.kotlinpoet.TypeSpec

internal interface ResponseAdapterBuilder {

  fun prepare()

  fun build(): List<TypeSpec>

  companion object {
    fun create(
        context: KotlinContext,
        modelGroup: IrModelGroup,
        path: List<String>,
        public: Boolean,
    ): ResponseAdapterBuilder = when {
      modelGroup.models.size == 1 -> {
        val model = modelGroup.models.first()

        ImplementationAdapterBuilder(
            context = context,
            model = model,
            path = path,
            addTypenameArgument = false,
            public = public
        )
      }
      modelGroup.models.size > 1 -> PolymorphicFieldResponseAdapterBuilder(
          context = context,
          modelGroup = modelGroup,
          path = path,
          public = public
      )
      else -> error("Don't know how to create an adapter for a scalar type")
    }
  }
}
