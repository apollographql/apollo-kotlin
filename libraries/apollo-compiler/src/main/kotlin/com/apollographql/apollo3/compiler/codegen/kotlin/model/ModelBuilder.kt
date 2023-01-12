package com.apollographql.apollo3.compiler.codegen.kotlin.model

import com.apollographql.apollo3.compiler.TargetLanguage.KOTLIN_1_5
import com.apollographql.apollo3.compiler.applyIf
import com.apollographql.apollo3.compiler.codegen.CodegenLayout.Companion.upperCamelCaseIgnoringNonLetters
import com.apollographql.apollo3.compiler.codegen.kotlin.KotlinContext
import com.apollographql.apollo3.compiler.codegen.kotlin.KotlinSymbols
import com.apollographql.apollo3.compiler.codegen.kotlin.adapter.from
import com.apollographql.apollo3.compiler.codegen.kotlin.helpers.makeDataClassFromProperties
import com.apollographql.apollo3.compiler.codegen.kotlin.helpers.maybeAddDeprecation
import com.apollographql.apollo3.compiler.codegen.kotlin.helpers.maybeAddDescription
import com.apollographql.apollo3.compiler.codegen.kotlin.helpers.maybeAddRequiresOptIn
import com.apollographql.apollo3.compiler.decapitalizeFirstLetter
import com.apollographql.apollo3.compiler.ir.IrAccessor
import com.apollographql.apollo3.compiler.ir.IrFragmentAccessor
import com.apollographql.apollo3.compiler.ir.IrModel
import com.apollographql.apollo3.compiler.ir.IrSubtypeAccessor
import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec

internal class ModelBuilder(
    private val context: KotlinContext,
    private val model: IrModel,
    private val superClassName: ClassName?,
    private val path: List<String>,
    private val hasSubclassesInSamePackage: Boolean,
    private val adaptableWith: String?,
    private val reservedNames: Set<String> = emptySet()
) {
  private val nestedBuilders = model.modelGroups.flatMap {
    it.models.map {
      ModelBuilder(
          context = context,
          model = it,
          superClassName = null,
          path = path + model.modelName,
          hasSubclassesInSamePackage = hasSubclassesInSamePackage,
          adaptableWith = null
      )
    }
  }
  private val simpleName = if (reservedNames.contains(model.modelName)) "${model.modelName}_" else model.modelName

  fun prepare() {
    context.resolver.registerModel(
        model.id,
        ClassName.from(path + simpleName)
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
          context.resolver.resolveIrType(it.info.type)
      )
          .applyIf(it.override) { addModifiers(KModifier.OVERRIDE) }
          .maybeAddDescription(it.info.description)
          .maybeAddDeprecation(it.info.deprecationReason)
          .maybeAddRequiresOptIn(context.resolver, it.info.optInFeature)
          .build()
    }

    val superInterfaces = implements.map {
      context.resolver.resolveModel(it)
    } + listOfNotNull(superClassName)

    val typeSpecBuilder = if (isInterface) {
      TypeSpec.interfaceBuilder(simpleName)
          // All interfaces can be sealed except if implementations exist in different packages (not allowed in Kotlin)
          .applyIf(context.isTargetLanguageVersionAtLeast(KOTLIN_1_5) && hasSubclassesInSamePackage) {
            addModifiers(KModifier.SEALED)
          }
          .addProperties(properties)
    } else {
      TypeSpec.classBuilder(simpleName)
          .makeDataClassFromProperties(properties)
    }

    val nestedTypes = nestedBuilders.map { it.build() }

    return typeSpecBuilder
        .applyIf(adaptableWith != null) {
          val annotationSpec: AnnotationSpec = AnnotationSpec.builder(KotlinSymbols.ApolloAdaptableWith)
              .addMember(CodeBlock.of("%T::class", context.resolver.resolveModelAdapter(adaptableWith!!)))
              .build ()
          addAnnotation(annotationSpec)
        }
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
          .addCode("return·this as? %T\n", context.resolver.resolveModel(accessor.returnedModelId))
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
