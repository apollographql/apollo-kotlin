package com.apollographql.apollo.compiler.codegen.kotlin.operations.util

import com.apollographql.apollo.compiler.codegen.Identifier
import com.apollographql.apollo.compiler.codegen.kotlin.KotlinContext
import com.apollographql.apollo.compiler.codegen.kotlin.KotlinSymbols
import com.apollographql.apollo.compiler.codegen.kotlin.helpers.HoistedExpressions
import com.apollographql.apollo.compiler.codegen.kotlin.helpers.addSuppressions
import com.apollographql.apollo.compiler.codegen.kotlin.helpers.from
import com.apollographql.apollo.compiler.codegen.kotlin.helpers.readFromResponseCodeBlock
import com.apollographql.apollo.compiler.codegen.kotlin.helpers.writeToResponseCodeBlock
import com.apollographql.apollo.compiler.internal.applyIf
import com.apollographql.apollo.compiler.ir.IrModel
import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.joinToCode

/**
 * @param addTypenameArgument whether to add a `typename` argument to the fromJson method
 * If no typename is present, we can make the generated adapter implement the [Adapter] interface
 * so that extension functions like `.obj(buffered)` can be used.
 */
internal class ImplementationAdapterBuilder(
    private val context: KotlinContext,
    private val model: IrModel,
    private val path: List<String>,
    private val addTypenameArgument: Boolean,
    private val public: Boolean,
) : ResponseAdapterBuilder {
  private val adapterName = model.modelName
  private val adaptedClassName by lazy {
    context.resolver.resolveModel(model.id)
  }

  private val nestedAdapterBuilders = model.modelGroups.map {
    ResponseAdapterBuilder.create(
        context,
        it,
        path + adapterName,
        false
    )
  }

  override fun prepare() {
    nestedAdapterBuilders.map { it.prepare() }
    context.resolver.registerModelAdapter(
        model.id,
        ClassName.from(path + adapterName)
    )
  }

  private fun responseNamesPropertySpec(model: IrModel): PropertySpec? {
    val regularProperties = model.properties.filter { !it.isSynthetic }

    if (regularProperties.isEmpty()) {
      return null
    }
    val initializer = regularProperties.map {
      CodeBlock.of("%S", it.info.responseName)
    }.joinToCode(prefix = "listOf(", separator = ", ", suffix = ")")

    return PropertySpec.builder(Identifier.RESPONSE_NAMES, KotlinSymbols.List.parameterizedBy(KotlinSymbols.String))
        .initializer(initializer)
        .build()
  }

  override fun build(): List<TypeSpec> {
    /**
     * `fromJson` and `toJson` share one collector so that a chain used by both is built once. The functions have to
     * be built before the hoisted properties can be read back.
     */
    val hoisted = HoistedExpressions()
    val fromJson = readFromResponseFunSpec(hoisted)
    val toJson = writeToResponseFunSpec(hoisted)

    return TypeSpec.objectBuilder(adapterName)
        .apply {
          val responseNames = responseNamesPropertySpec(model)
          if (responseNames != null) {
            addProperty(responseNames)
          }
        }
        .addProperties(hoisted.propertySpecs())
        .addFunction(fromJson)
        .addFunction(toJson)
        .addTypes(nestedAdapterBuilders.flatMap { it.build() })
        .apply {
          if (!addTypenameArgument) {
            addSuperinterface(
                KotlinSymbols.Adapter.parameterizedBy(
                    context.resolver.resolveModel(model.id)
                )
            )
          }
          if (!public) {
            addModifiers(KModifier.PRIVATE)
          }
        }
        .build()
        .let { listOf(it) }

  }

  private fun readFromResponseFunSpec(hoisted: HoistedExpressions): FunSpec {
    return FunSpec.builder(Identifier.fromJson)
        .returns(adaptedClassName)
        .addParameter(Identifier.reader, KotlinSymbols.JsonReader)
        .addParameter(
            ParameterSpec.builder(Identifier.customScalarAdapters, KotlinSymbols.CustomScalarAdapters)
                .applyIf(addTypenameArgument) {
                  addAnnotation(AnnotationSpec.builder(KotlinSymbols.Suppress).addMember("%S", "UNUSED_PARAMETER").build())
                }
                .build()
        )
        .apply {
          if (addTypenameArgument) {
            addParameter(
                ParameterSpec.builder(
                    Identifier.typename,
                    KotlinSymbols.String
                ).build()
            )
          } else {
            addModifiers(KModifier.OVERRIDE)
          }
        }
        .addCode(readFromResponseCodeBlock(model, context, addTypenameArgument, hoisted))
        .build()
  }

  private fun writeToResponseFunSpec(hoisted: HoistedExpressions): FunSpec {
    return FunSpec.builder(Identifier.toJson)
        .addParameter(Identifier.writer, KotlinSymbols.JsonWriter)
        .addParameter(
            ParameterSpec.builder(Identifier.customScalarAdapters, KotlinSymbols.CustomScalarAdapters)
                .applyIf(addTypenameArgument) {
                  addSuppressions(unusedParameter = true)
                }
                .build()
        )
        .addParameter(Identifier.value, adaptedClassName)
        .addCode(writeToResponseCodeBlock(model, context, hoisted))
        .apply {
          addSuppressions(
              deprecation = model.properties.any { it.info.deprecationReason != null },
              optInUsage = model.properties.any { it.info.optInFeature != null }
          )

          if (!addTypenameArgument) {
            addModifiers(KModifier.OVERRIDE)
          }
        }
        .build()
  }
}
