package com.apollographql.apollo3.compiler.codegen.java.operations

import com.apollographql.apollo3.compiler.codegen.Identifier
import com.apollographql.apollo3.compiler.codegen.java.JavaClassNames
import com.apollographql.apollo3.compiler.codegen.java.JavaOperationsContext
import com.apollographql.apollo3.compiler.codegen.java.L
import com.apollographql.apollo3.compiler.codegen.java.S
import com.apollographql.apollo3.compiler.codegen.java.T
import com.apollographql.apollo3.compiler.codegen.java.helpers.codeBlock
import com.apollographql.apollo3.compiler.codegen.java.helpers.toListInitializerCodeblock
import com.apollographql.apollo3.compiler.ir.BVariable
import com.apollographql.apollo3.compiler.ir.BooleanExpression
import com.apollographql.apollo3.compiler.ir.IrArgument
import com.apollographql.apollo3.compiler.ir.IrArgumentDefinition
import com.apollographql.apollo3.compiler.ir.IrField
import com.apollographql.apollo3.compiler.ir.IrFragment
import com.apollographql.apollo3.compiler.ir.IrSelection
import com.apollographql.apollo3.compiler.ir.IrSelectionSet
import com.squareup.javapoet.CodeBlock
import com.squareup.javapoet.FieldSpec
import com.squareup.javapoet.ParameterizedTypeName
import com.squareup.javapoet.TypeSpec
import javax.lang.model.element.Modifier

internal class CompiledSelectionsBuilder(
    private val context: JavaOperationsContext,
) {

  fun build(selectionSets: List<IrSelectionSet>, rootName: String): TypeSpec {
    return TypeSpec.classBuilder(rootName)
        .addModifiers(Modifier.PUBLIC)
        .addFields(selectionSets.map { it.fieldSpec() })
        .build()
  }

  private fun IrSelectionSet.fieldSpec(): FieldSpec {
    val propertyName = "__$name"

    return FieldSpec.builder(ParameterizedTypeName.get(JavaClassNames.List, JavaClassNames.CompiledSelection), propertyName)
        .initializer(selections.map { it.codeBlock() }.toListInitializerCodeblock(withNewLines = true))
        .addModifiers(Modifier.STATIC)
        .addModifiers(if (isRoot) Modifier.PUBLIC else Modifier.PRIVATE)
        .build()
  }

  private fun IrSelection.codeBlock() = when (this) {
    is IrField -> this.codeBlock()
    is IrFragment -> this.codeBlock()
  }

  private fun IrField.codeBlock(): CodeBlock {
    val builder = CodeBlock.builder()
    builder.add("new $T($S, $L)", JavaClassNames.CompiledFieldBuilder, name, type.codeBlock(context))
    builder.indent()

    if (alias != null) {
      builder.add(".alias($S)", alias)
    }
    if (condition != BooleanExpression.True) {
      builder.add(".condition($L)", condition.toCompiledConditionInitializer())
    }
    if (arguments.isNotEmpty()) {
      builder.add(".arguments($L)", arguments.sortedBy { it.definition.name }.map { it.codeBlock() }.toListInitializerCodeblock())
    }
    if (selectionSetName != null) {
      builder.add(".selections($L)", "__$selectionSetName")
    }
    builder.unindent()
    builder.add(".build()")

    return builder.build()
  }

  private fun IrFragment.codeBlock(): CodeBlock {
    val builder = CodeBlock.builder()
    builder.add(
        "new $T($S, $L)",
        JavaClassNames.CompiledFragmentBuilder,
        typeCondition,
        possibleTypes.map { CodeBlock.of(S, it) }.toListInitializerCodeblock(false)
    )
    builder.indent()
    if (condition !is BooleanExpression.True) {
      builder.add(".condition($L)", condition.toCompiledConditionInitializer())
    }
    if (selectionSetName != null) {
      builder.add(".selections($L)", "__$selectionSetName")
    } else {
      check(name != null)
      builder.add(".selections($T.${Identifier.root})", context.resolver.resolveFragmentSelections(name))
    }

    builder.unindent()
    builder.add(".build()")

    return builder.build()
  }

  private fun BooleanExpression<BVariable>.toCompiledConditionInitializer(): CodeBlock {
    val conditions = when (this) {
      is BooleanExpression.And -> operands.map { it.singleInitializer() }
      else -> listOf(singleInitializer())
    }

    return conditions.toListInitializerCodeblock()
  }

  private fun BooleanExpression<BVariable>.singleInitializer(): CodeBlock {
    var expression = this
    var inverted = false
    if (this is BooleanExpression.Not) {
      expression = this.operand
      inverted = true
    }

    check(expression is BooleanExpression.Element)

    return CodeBlock.of("new $T($S, $L)", JavaClassNames.CompiledCondition, expression.value.name, inverted.toString())
  }

  private fun IrArgumentDefinition.codeBlock(): CodeBlock {
    val builder = CodeBlock.builder()
    builder.add(
        "new $T($S)",
        JavaClassNames.CompiledArgumentDefinition,
        name,
    )
    if (isKey) {
      builder.add(".isKey(true)")
    }
    if (isPagination) {
      builder.add(".isPagination(true)")
    }
    builder.add(".build()")
    return builder.build()
  }

  private fun IrArgument.codeBlock(): CodeBlock {
    val argumentBuilder = CodeBlock.builder()
    argumentBuilder.add(
        "new $T($L)",
        JavaClassNames.CompiledArgument,
        definition.codeBlock(),
    )
    if (this.value != null) {
      argumentBuilder.add(".value($L)", value.codeBlock())
    }
    argumentBuilder.add(".build()")

    return argumentBuilder.build()
  }

}
