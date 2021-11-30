package com.apollographql.apollo3.compiler.codegen.java.model

import com.apollographql.apollo3.compiler.applyIf
import com.apollographql.apollo3.compiler.codegen.CodegenLayout.Companion.upperCamelCaseIgnoringNonLetters
import com.apollographql.apollo3.compiler.codegen.java.JavaClassNames
import com.apollographql.apollo3.compiler.codegen.java.JavaContext
import com.apollographql.apollo3.compiler.codegen.java.adapter.toClassName
import com.apollographql.apollo3.compiler.codegen.java.helpers.makeDataClassFromProperties
import com.apollographql.apollo3.compiler.codegen.java.helpers.maybeAddDeprecation
import com.apollographql.apollo3.compiler.codegen.java.helpers.maybeAddDescription
import com.apollographql.apollo3.compiler.decapitalizeFirstLetter
import com.apollographql.apollo3.compiler.ir.IrAccessor
import com.apollographql.apollo3.compiler.ir.IrFragmentAccessor
import com.apollographql.apollo3.compiler.ir.IrModel
import com.apollographql.apollo3.compiler.ir.IrSubtypeAccessor
import com.squareup.javapoet.ClassName
import com.squareup.javapoet.FieldSpec
import com.squareup.javapoet.TypeSpec
import javax.lang.model.element.Modifier

/**
 * @param path: the path leading to this model but not including the model name
 */
class ModelBuilder(
    private val context: JavaContext,
    private val model: IrModel,
    private val superClassName: ClassName?,
    private val path: List<String>,
) {
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
        (path + model.modelName).toClassName()
    )
    nestedBuilders.forEach { it.prepare() }
  }

  fun build(): TypeSpec {
    return model.typeSpec()
  }

  fun IrModel.typeSpec(): TypeSpec {
    val fields = properties.filter { !it.hidden }.map {
      FieldSpec.builder(
          context.resolver.resolveIrType(it.info.type),
          context.layout.propertyName(it.info.responseName),
      )
          .addModifiers(Modifier.PUBLIC)
          .applyIf(it.override) {
            addAnnotation(JavaClassNames.Override)
          }
          .maybeAddDescription(it.info.description)
          .maybeAddDeprecation(it.info.deprecationReason)
          .build()
    }

    val superInterfaces = implements.map {
      context.resolver.resolveModel(it)
    } + listOfNotNull(superClassName)

    val typeSpecBuilder = if (isInterface) {
      TypeSpec.interfaceBuilder(modelName)
          .addFields(fields)
    } else {
      TypeSpec.classBuilder(modelName)
          .makeDataClassFromProperties(fields)
    }

    val nestedTypes = nestedBuilders.map { it.build() }

    return typeSpecBuilder
        .addModifiers(Modifier.PUBLIC)
        .applyIf(path.size > 1) {
          // path is the path leading to this model. E.g listOf(packageName, TopLevel, Data, Nested, ...)
          // Java doesn't understand static top level classes
          addModifiers(Modifier.STATIC)
        }
        .addTypes(nestedTypes)
        .addSuperinterfaces(superInterfaces)
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
