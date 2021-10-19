package com.apollographql.apollo3.compiler.codegen.kotlin.test

import com.apollographql.apollo3.compiler.applyIf
import com.apollographql.apollo3.compiler.codegen.kotlin.KotlinClassNames
import com.apollographql.apollo3.compiler.codegen.kotlin.KotlinContext
import com.apollographql.apollo3.compiler.codegen.kotlin.adapter.ResponseAdapterBuilder
import com.apollographql.apollo3.compiler.codegen.kotlin.adapter.from
import com.apollographql.apollo3.compiler.ir.IrModel
import com.apollographql.apollo3.compiler.ir.IrModelGroup
import com.apollographql.apollo3.compiler.ir.IrProperty
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec

class TestBuilderBuilder(
    private val context: KotlinContext,
    private val model: IrModel,
    private val path: List<String>,
    private val inner: Boolean,
) {
  private val builderName = model.modelName

  private val nestedAdapterBuilders = model.modelGroups.flatMap { it.models }.map {
    TestBuilderBuilder(
        context,
        it,
        path + builderName,
        true
    )
  }

  fun prepare() {
    context.resolver.registerTestBuilder(
        model.id,
        ClassName.from(path + builderName)
    )
    nestedAdapterBuilders.forEach { it.prepare() }
  }

  fun build(): TypeSpec {
    return TypeSpec.classBuilder(builderName)
        .applyIf(inner) {
          addModifiers(KModifier.INNER)
        }
        .superclass(KotlinClassNames.MapBuilder)
        .addProperties(
            model.properties.map { it.propertySpec() }
        )
        .build()
  }

  private fun IrProperty.propertySpec(): PropertySpec {
    return PropertySpec.builder(this.info.responseName)
  }
}