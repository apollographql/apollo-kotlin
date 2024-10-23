package com.apollographql.apollo.compiler.codegen.kotlin.operations

import com.apollographql.apollo.compiler.codegen.Identifier
import com.apollographql.apollo.compiler.codegen.kotlin.KotlinOperationsContext
import com.apollographql.apollo.compiler.codegen.kotlin.KotlinSymbols
import com.apollographql.apollo.compiler.codegen.kotlin.helpers.codeBlock
import com.apollographql.apollo.compiler.codegen.kotlin.helpers.toListInitializerCodeblock
import com.apollographql.apollo.compiler.internal.applyIf
import com.apollographql.apollo.compiler.ir.BVariable
import com.apollographql.apollo.compiler.ir.BooleanExpression
import com.apollographql.apollo.compiler.ir.IrArgument
import com.apollographql.apollo.compiler.ir.IrField
import com.apollographql.apollo.compiler.ir.IrFragment
import com.apollographql.apollo.compiler.ir.IrSelection
import com.apollographql.apollo.compiler.ir.IrSelectionSet
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.joinToCode

internal class CompiledSelectionsBuilder(
    private val context: KotlinOperationsContext,
) {
  fun build(selectionSets: List<IrSelectionSet>, rootName: String): TypeSpec {
    return TypeSpec.objectBuilder(rootName)
        .addProperties(selectionSets.map { it.toPropertySpec() })
        .build()
  }

  private fun IrSelectionSet.toPropertySpec(): PropertySpec {
    val propertyName = "__$name"

    return PropertySpec.builder(propertyName, KotlinSymbols.List.parameterizedBy(KotlinSymbols.CompiledSelection))
        .initializer(selections.map { it.codeBlock() }.toListInitializerCodeblock(true))
        .applyIf(!isRoot) {
          addModifiers(KModifier.PRIVATE)
        }
        .build()
  }

  private fun IrSelection.codeBlock() = when(this) {
    is IrField -> this.codeBlock()
    is IrFragment -> this.codeBlock()
  }

  private fun IrField.codeBlock(): CodeBlock {
    val builder = CodeBlock.builder()
    builder.add("%T(\n", KotlinSymbols.CompiledFieldBuilder)
    builder.indent()
    builder.add("name = %S,\n", name)
    builder.add(
        CodeBlock.of("type = %L\n", type.codeBlock(context))
    )
    builder.unindent()
    builder.add(")")

    if (alias != null) {
      builder.add(".alias(%S)\n", alias)
    }
    if (condition != BooleanExpression.True) {
      builder.add(".condition(%L)\n", condition.toCompiledConditionInitializer())
    }
    if (arguments.isNotEmpty()) {
      builder.add(".arguments(%L)\n", arguments.sortedBy { it.definitionId }.map { it.codeBlock() }.toListInitializerCodeblock(true))
    }
    if (selectionSetName != null) {
      builder.add(".selections(%N)\n", "__$selectionSetName")
    }
    builder.add(".build()")

    return builder.build()
  }

  private fun IrFragment.codeBlock(): CodeBlock {
    val builder = CodeBlock.builder()
    builder.add("%T(\n", KotlinSymbols.CompiledFragmentBuilder)
    builder.indent()
    builder.add("typeCondition = %S,\n", typeCondition)
    builder.add("possibleTypes = %L\n", possibleTypes.map { CodeBlock.of("%S", it) }.toListInitializerCodeblock(false))
    builder.unindent()
    builder.add(")")

    if (condition !is BooleanExpression.True) {
      builder.add(".condition(%L)\n", condition.toCompiledConditionInitializer())
    }
    if (selectionSetName != null) {
      builder.add(".selections(%N)\n", "__$selectionSetName")
    } else {
      check (name != null)
      builder.add(".selections(%T.${Identifier.root})\n", context.resolver.resolveFragmentSelections(name))
    }
    builder.add(".build()")

    return builder.build()
  }

  private fun BooleanExpression<BVariable>.toCompiledConditionInitializer(): CodeBlock {
    val conditions = when (this) {
      is BooleanExpression.And -> operands.map { it.singleInitializer() }
      else -> listOf(singleInitializer())
    }

    return CodeBlock.builder()
        .add("listOf(")
        .add(conditions.joinToCode(", "))
        .add(")")
        .build()
  }

  private fun BooleanExpression<BVariable>.singleInitializer(): CodeBlock {
    var expression = this
    var inverted = false
    if (this is BooleanExpression.Not) {
      expression = this.operand
      inverted = true
    }

    check(expression is BooleanExpression.Element)

    return CodeBlock.of("%T(%S, %L)", KotlinSymbols.CompiledCondition, expression.value.name, inverted.toString())
  }

  private fun IrArgument.codeBlock(): CodeBlock {
    val argumentBuilder = CodeBlock.builder()
    argumentBuilder.add(
        "%T(%T.%N)",
        KotlinSymbols.CompiledArgument,
        context.resolver.resolveArgumentDefinition(definitionId),
        definitionPropertyName,
    )

    if (this.value != null) {
      argumentBuilder.add(".value(%L)", value.codeBlock())
    }
    argumentBuilder.add(".build()")
    return argumentBuilder.build()
  }
}
