package com.apollographql.apollo3.compiler.codegen.kotlin.selections

import com.apollographql.apollo3.api.BVariable
import com.apollographql.apollo3.api.BooleanExpression
import com.apollographql.apollo3.compiler.applyIf
import com.apollographql.apollo3.compiler.codegen.Identifier.root
import com.apollographql.apollo3.compiler.codegen.kotlin.KotlinContext
import com.apollographql.apollo3.compiler.codegen.kotlin.KotlinSymbols
import com.apollographql.apollo3.compiler.codegen.kotlin.helpers.codeBlock
import com.apollographql.apollo3.compiler.codegen.kotlin.helpers.toListInitializerCodeblock
import com.apollographql.apollo3.compiler.ir.IrArgument
import com.apollographql.apollo3.compiler.ir.IrField
import com.apollographql.apollo3.compiler.ir.IrFragment
import com.apollographql.apollo3.compiler.ir.IrSelection
import com.apollographql.apollo3.compiler.ir.IrSelectionSet
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.joinToCode

internal class CompiledSelectionsBuilder(
    private val context: KotlinContext,
) {
  fun build(selectionSets: List<IrSelectionSet>, rootName: String): TypeSpec {
    return TypeSpec.objectBuilder(rootName)
        .addProperties(selectionSets.map { it.toPropertySpec() })
        .build()
  }

  private fun IrSelectionSet.toPropertySpec(): PropertySpec {
    val propertyName = context.layout.compiledSelectionsName(name)

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
    builder.add("name·=·%S,\n", name)
    builder.add(
        CodeBlock.of("type·=·%L\n", type.codeBlock(context))
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
      builder.add(".arguments(%L)\n", arguments.sortedBy { it.name }.map { it.codeBlock() }.toListInitializerCodeblock(true))
    }
    if (selectionSetName != null) {
      builder.add(".selections(%N)\n", context.layout.compiledSelectionsName(selectionSetName))
    }
    builder.add(".build()")

    return builder.build()
  }

  private fun IrFragment.codeBlock(): CodeBlock {
    val builder = CodeBlock.builder()
    builder.add("%T(\n", KotlinSymbols.CompiledFragmentBuilder)
    builder.indent()
    builder.add("typeCondition·=·%S,\n", typeCondition)
    builder.add("possibleTypes·=·%L\n", possibleTypes.map { CodeBlock.of("%S", it) }.toListInitializerCodeblock(false))
    builder.unindent()
    builder.add(")")

    if (condition !is BooleanExpression.True) {
      builder.add(".condition(%L)\n", condition.toCompiledConditionInitializer())
    }
    if (selectionSetName != null) {
      builder.add(".selections(%N)\n", context.layout.compiledSelectionsName(selectionSetName))
    } else {
      check (name != null)
      builder.add(".selections(%T.$root)\n", context.resolver.resolveFragmentSelections(name))
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

    return CodeBlock.of("%T(%S,·%L)", KotlinSymbols.CompiledCondition, expression.value.name, inverted.toString())
  }

  private fun IrArgument.codeBlock(): CodeBlock {
    val argumentBuilder = CodeBlock.builder()
    argumentBuilder.add(
        "%T(%S,·%L)",
        KotlinSymbols.CompiledArgument,
        name,
        value.codeBlock()
    )

    if (isKey) {
      argumentBuilder.add(".isKey(true)")
    }
    if (isPagination) {
      argumentBuilder.add(".isPagination(true)")
    }
    argumentBuilder.add(".build()")
    return argumentBuilder.build()
  }
}

