package com.apollographql.apollo.compiler.codegen.kotlin.operations

import com.apollographql.apollo.compiler.codegen.kotlin.KotlinOperationsContext
import com.apollographql.apollo.compiler.codegen.kotlin.KotlinSymbols
import com.apollographql.apollo.compiler.codegen.kotlin.helpers.from
import com.apollographql.apollo.compiler.codegen.kotlin.helpers.makeClassFromProperties
import com.apollographql.apollo.compiler.codegen.kotlin.helpers.maybeAddDeprecation
import com.apollographql.apollo.compiler.codegen.kotlin.helpers.maybeAddDescription
import com.apollographql.apollo.compiler.codegen.kotlin.helpers.maybeAddJsExport
import com.apollographql.apollo.compiler.codegen.kotlin.helpers.maybeAddRequiresOptIn
import com.apollographql.apollo.compiler.decapitalizeFirstLetter
import com.apollographql.apollo.compiler.internal.applyIf
import com.apollographql.apollo.compiler.ir.IrAccessor
import com.apollographql.apollo.compiler.ir.IrFragmentAccessor
import com.apollographql.apollo.compiler.ir.IrModel
import com.apollographql.apollo.compiler.ir.IrSubtypeAccessor
import com.apollographql.apollo.compiler.upperCamelCaseIgnoringNonLetters
import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec

internal class ModelBuilder(
    private val context: KotlinOperationsContext,
    private val model: IrModel,
    private val superClassName: ClassName?,
    private val path: List<String>,
    private val hasSubclassesInSamePackage: Boolean,
    private val adaptableWith: String?,
    private val reservedNames: Set<String> = emptySet(),
    private val isTopLevel: Boolean = false
) {
  private val nestedBuilders = model.modelGroups.flatMap { modelGroup ->
    modelGroup.models.map {
      ModelBuilder(
          context = context,
          model = it,
          superClassName = null,
          path = path + model.modelName,
          hasSubclassesInSamePackage = hasSubclassesInSamePackage,
          adaptableWith = null,
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
          context.resolver.resolveIrType(it.info.type, jsExport = context.jsExport, isInterface = isInterface)
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
          .applyIf(hasSubclassesInSamePackage) {
            addModifiers(KModifier.SEALED)
          }
          .addProperties(properties)
    } else {
      TypeSpec.classBuilder(simpleName)
          .makeClassFromProperties(
              context.generateMethods,
              properties,
              context.resolver.resolveModel(model.id)
          )
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
        .applyIf(isTopLevel) { maybeAddJsExport(context) }
        .applyIf(accessors.isNotEmpty()) {
          val accessorFunSpecs = buildAccessorFunSpecs(this@typeSpec)
          if (!context.jsExport) {
            addType(
              TypeSpec.companionObjectBuilder()
              .addFunctions(accessorFunSpecs)
              .build()
            )
          }
        }
        .addSuperinterfaces(superInterfaces)
        .build()
  }

  private fun buildAccessorFunSpecs(model: IrModel): List<FunSpec> {
    return model.accessors.map { accessor ->

      val returnedClassName = context.resolver.resolveModel(accessor.returnedModelId)
      FunSpec.builder(accessor.funName())
          .applyIf(!context.jsExport) {
            receiver(context.resolver.resolveModel(model.id))
          }
          .returns(returnedClassName.copy(nullable = true))
          .addAnnotation(AnnotationSpec.builder(KotlinSymbols.Suppress).addMember("%S", "USELESS_CAST").build())
          // https://github.com/square/kotlinpoet/pull/1559
          .addCode("return this as? %T\n", returnedClassName)
          .build()
    }
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