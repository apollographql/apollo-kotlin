package com.apollographql.apollo.compiler.codegen.kotlin.operations.util

import com.apollographql.apollo.compiler.codegen.Identifier
import com.apollographql.apollo.compiler.codegen.Identifier.ADAPTER
import com.apollographql.apollo.compiler.codegen.Identifier.ROOT_FIELD
import com.apollographql.apollo.compiler.codegen.Identifier.customScalarAdapters
import com.apollographql.apollo.compiler.codegen.Identifier.root
import com.apollographql.apollo.compiler.codegen.Identifier.rootField
import com.apollographql.apollo.compiler.codegen.Identifier.selections
import com.apollographql.apollo.compiler.codegen.Identifier.serializeVariables
import com.apollographql.apollo.compiler.codegen.Identifier.withDefaultValues
import com.apollographql.apollo.compiler.codegen.Identifier.writer
import com.apollographql.apollo.compiler.codegen.kotlin.KotlinOperationsContext
import com.apollographql.apollo.compiler.codegen.kotlin.KotlinSymbols
import com.apollographql.apollo.compiler.codegen.kotlin.helpers.patchKotlinNativeOptionalArrayProperties
import com.apollographql.apollo.compiler.ir.IrExecutable
import com.apollographql.apollo.compiler.ir.IrFragmentDefinition
import com.apollographql.apollo.compiler.ir.IrOperationDefinition
import com.apollographql.apollo.compiler.ir.IrProperty
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.TypeSpec

private const val rootFieldHolder = "__RootField"
private const val rootFieldHolderProperty = "instance"

internal fun serializeVariablesFunSpec(
    adapterClassName: TypeName?,
    emptyMessage: String,
): FunSpec {

  val body = if (adapterClassName == null) {
    CodeBlock.of("""
      // $emptyMessage
    """.trimIndent())
  } else {
    CodeBlock.of(
        "%L.$serializeVariables($writer, this, $customScalarAdapters, $withDefaultValues)",
        CodeBlock.of("%T", adapterClassName)
    )
  }
  return FunSpec.builder(serializeVariables)
      .addModifiers(KModifier.OVERRIDE)
      .addParameter(writer, KotlinSymbols.JsonWriter)
      .addParameter(customScalarAdapters, KotlinSymbols.CustomScalarAdapters)
      .addParameter(withDefaultValues, KotlinSymbols.Boolean)
      .addCode(body)
      .build()
}

internal fun adapterFunSpec(
    context: KotlinOperationsContext,
    property: IrProperty,
): FunSpec {
  val type = property.info.type

  return FunSpec.builder("adapter")
      .addModifiers(KModifier.OVERRIDE)
      .returns(KotlinSymbols.Adapter.parameterizedBy(context.resolver.resolveIrType(type, context.jsExport)))
      .addCode("return $ADAPTER")
      .build()
}

internal fun rootFieldFunSpec(): FunSpec {
  return FunSpec.builder(rootField)
      .addModifiers(KModifier.OVERRIDE)
      .returns(KotlinSymbols.CompiledField)
      .addCode("return $ROOT_FIELD")
      .build()
}

internal fun TypeSpec.Builder.executableCompanion(context: KotlinOperationsContext, executable: IrExecutable) = apply {
  return addSuperinterface(KotlinSymbols.ExecutableDefinition.parameterizedBy(context.resolver.resolveModel(executable.dataModelGroup.baseModelId)))
      .addProperty(adadpterPropertySpec(context, executable))
      .addProperty(rootFieldPropertySpec())
      .addType(rootFieldHolderTypeSpec(context, executable))
}


internal fun adadpterPropertySpec(context: KotlinOperationsContext, executable: IrExecutable): PropertySpec {
  return PropertySpec.builder(
      ADAPTER,
      KotlinSymbols.Adapter.parameterizedBy(context.resolver.resolveIrType(executable.dataProperty.info.type, context.jsExport))
  ).addModifiers(KModifier.OVERRIDE)
      .initializer(context.resolver.adapterInitializer(executable.dataProperty.info.type, executable.dataProperty.requiresBuffering, context.jsExport))
      .build()
}
/**
 * [ROOT_FIELD] reads through [rootFieldHolder] instead of being initialized in the companion object so that
 * reading [ADAPTER] doesn't build the whole selections tree. Both properties live in the same companion
 * object, so a plain initializer here would make every parse materialize every
 * [KotlinSymbols.CompiledField] of the executable, including for operations that never hit the normalized
 * cache. A separate object has its own class initializer, so the tree is only built if [ROOT_FIELD] is
 * actually read - and unlike `by lazy`, it costs no wrapper allocation and no synchronization per read.
 */
internal fun rootFieldPropertySpec(): PropertySpec {
  return PropertySpec.builder(ROOT_FIELD, KotlinSymbols.CompiledField)
      .addModifiers(KModifier.OVERRIDE)
      .getter(
          FunSpec.getterBuilder()
              .addCode("return %L.%L\n", rootFieldHolder, rootFieldHolderProperty)
              .build()
      )
      .build()
}

private fun rootFieldHolderTypeSpec(context: KotlinOperationsContext, executable: IrExecutable): TypeSpec {
  val selectionsClassName = when (executable) {
    is IrOperationDefinition -> context.resolver.resolveOperationSelections(executable.name)
    is IrFragmentDefinition -> context.resolver.resolveFragmentSelections(executable.name)
  }

  return TypeSpec.objectBuilder(rootFieldHolder)
      .addModifiers(KModifier.PRIVATE)
      .addProperty(
          PropertySpec.builder(rootFieldHolderProperty, KotlinSymbols.CompiledField)
              .initializer(
                  CodeBlock.builder()
                      .add("%T(\n", KotlinSymbols.CompiledFieldBuilder)
                      .indent()
                      .add("name = %S,\n", Identifier.data)
                      .add("type = %L\n", context.resolver.resolveCompiledType(executable.typeCondition))
                      .unindent()
                      .add(")\n")
                      .add(".$selections(selections = %T.$root)\n", selectionsClassName)
                      .add(".build()\n")
                      .build()
              )
              .build()
      )
      .build()
}

internal fun TypeSpec.maybeAddFilterNotNull(generateFilterNotNull: Boolean): TypeSpec {
  if (!generateFilterNotNull) {
    return this
  }
  return patchKotlinNativeOptionalArrayProperties()
}
