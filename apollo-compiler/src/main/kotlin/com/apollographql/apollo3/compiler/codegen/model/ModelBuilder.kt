package com.apollographql.apollo3.compiler.codegen.model

import com.apollographql.apollo3.compiler.applyIf
import com.apollographql.apollo3.compiler.codegen.CgContext
import com.apollographql.apollo3.compiler.codegen.CgLayout.Companion.upperCamelCaseIgnoringNonLetters
import com.apollographql.apollo3.compiler.codegen.adapter.from
import com.apollographql.apollo3.compiler.codegen.helpers.makeDataClassFromProperties
import com.apollographql.apollo3.compiler.codegen.helpers.maybeAddDeprecation
import com.apollographql.apollo3.compiler.codegen.helpers.maybeAddDescription
import com.apollographql.apollo3.compiler.decapitalizeFirstLetter
import com.apollographql.apollo3.compiler.unified.ir.IrAccessor
import com.apollographql.apollo3.compiler.unified.ir.IrFragmentAccessor
import com.apollographql.apollo3.compiler.unified.ir.IrModel
import com.apollographql.apollo3.compiler.unified.ir.IrSubtypeAccessor
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec

class ModelBuilder(
    private val context: CgContext,
    private val model: IrModel,
    private val superClassName: ClassName?,
    private val path: List<String>
)  {
  private val nestedBuilders = model.modelGroups.flatMap {
    it.models.map {
      ModelBuilder(
          context,
          it,
          null,
          path + model.modelName
      )
    }
  }

  fun prepare() {
    context.resolver.registerModel(
        model.id,
        ClassName.from(path + model.modelName)
    )
    nestedBuilders.forEach { it.prepare() }
  }

  fun build(): TypeSpec {
    return model.typeSpec()
  }

  fun IrModel.typeSpec(): TypeSpec {
    val properties = properties.map {
      PropertySpec.builder(
          context.layout.propertyName(it.info.responseName),
          context.resolver.resolveType(it.info.type)
      )
          .applyIf(it.override) { addModifiers(KModifier.OVERRIDE) }
          .maybeAddDescription(it.info.description)
          .maybeAddDeprecation(it.info.deprecationReason)
          .build()
    }

    val superInterfaces = implements.map {
      context.resolver.resolveModel(it)
    } + listOfNotNull(superClassName)


    val typeSpecBuilder = if (isInterface) {
      TypeSpec.interfaceBuilder(modelName)
          .addProperties(properties)
    } else {
      TypeSpec.classBuilder(modelName)
          .makeDataClassFromProperties(properties)
    }

    val nestedTypes = nestedBuilders.map { it.build() }

    return typeSpecBuilder
        .addTypes(nestedTypes)
        .applyIf(accessors.isNotEmpty()) {
          addType(companionTypeSpec(this@typeSpec))
        }
        .addSuperinterfaces(superInterfaces)
        .build()
  }

  private fun companionTypeSpec(model: IrModel): TypeSpec {
      val funSpecs = model.accessors.map { accessor ->
        FunSpec.builder(accessor.funName())
            .receiver(context.resolver.resolveModel(model.id))
            .addCode("return this as? %T\n", context.resolver.resolveModel(accessor.returnedModelId))
            .build()
      }
      return TypeSpec.companionObjectBuilder()
          .addFunctions(funSpecs)
          .build()
  }

  private fun IrAccessor.funName(): String {
    return when (this) {
      is IrFragmentAccessor -> this.fragmentName.decapitalizeFirstLetter()
      is IrSubtypeAccessor -> {
        "as${upperCamelCaseIgnoringNonLetters(this.typeSet)}"
      }
    }
  }
}