package com.apollographql.apollo3.compiler.codegen.java.selections

import com.apollographql.apollo3.api.BVariable
import com.apollographql.apollo3.api.BooleanExpression
import com.apollographql.apollo3.ast.GQLArgument
import com.apollographql.apollo3.ast.GQLBooleanValue
import com.apollographql.apollo3.ast.GQLEnumValue
import com.apollographql.apollo3.ast.GQLField
import com.apollographql.apollo3.ast.GQLFloatValue
import com.apollographql.apollo3.ast.GQLFragmentDefinition
import com.apollographql.apollo3.ast.GQLFragmentSpread
import com.apollographql.apollo3.ast.GQLInlineFragment
import com.apollographql.apollo3.ast.GQLIntValue
import com.apollographql.apollo3.ast.GQLListType
import com.apollographql.apollo3.ast.GQLListValue
import com.apollographql.apollo3.ast.GQLNamedType
import com.apollographql.apollo3.ast.GQLNonNullType
import com.apollographql.apollo3.ast.GQLNullValue
import com.apollographql.apollo3.ast.GQLObjectValue
import com.apollographql.apollo3.ast.GQLSelection
import com.apollographql.apollo3.ast.GQLStringValue
import com.apollographql.apollo3.ast.GQLType
import com.apollographql.apollo3.ast.GQLValue
import com.apollographql.apollo3.ast.GQLVariableValue
import com.apollographql.apollo3.ast.Schema
import com.apollographql.apollo3.ast.definitionFromScope
import com.apollographql.apollo3.ast.leafType
import com.apollographql.apollo3.compiler.capitalizeFirstLetter
import com.apollographql.apollo3.compiler.codegen.Identifier.root
import com.apollographql.apollo3.compiler.codegen.java.JavaClassNames
import com.apollographql.apollo3.compiler.codegen.java.JavaContext
import com.apollographql.apollo3.compiler.codegen.java.L
import com.apollographql.apollo3.compiler.codegen.java.S
import com.apollographql.apollo3.compiler.codegen.java.T
import com.apollographql.apollo3.compiler.codegen.java.helpers.toListInitializerCodeblock
import com.apollographql.apollo3.compiler.codegen.java.helpers.toMapInitializerCodeblock
import com.apollographql.apollo3.compiler.codegen.keyArgs
import com.apollographql.apollo3.compiler.codegen.paginationArgs
import com.apollographql.apollo3.compiler.ir.toIncludeBooleanExpression
import com.squareup.javapoet.CodeBlock
import com.squareup.javapoet.FieldSpec
import com.squareup.javapoet.ParameterizedTypeName
import com.squareup.javapoet.TypeSpec
import javax.lang.model.element.Modifier

class CompiledSelectionsBuilder(
    private val context: JavaContext,
    private val allFragmentDefinitions: Map<String, GQLFragmentDefinition>,
    private val schema: Schema,
) {
  private val usedNames = mutableSetOf<String>()

  private fun resolveNameClashes(usedNames: MutableSet<String>, modelName: String): String {
    var i = 0
    var name = modelName
    while (usedNames.contains(name)) {
      i++
      name = "$modelName$i"
    }
    usedNames.add(name)
    return name
  }

  fun build(selections: List<GQLSelection>, rootName: String, parentType: String): TypeSpec {
    return TypeSpec.classBuilder(rootName)
        .addModifiers(Modifier.PUBLIC)
        .addFields(selections.walk(root, isRoot = true, parentType))
        .build()
  }

  private fun List<GQLSelection>.walk(name: String, isRoot: Boolean, parentType: String): List<FieldSpec> {
    val modelName = if (isRoot) root else context.layout.compiledSelectionsName(name)
    val propertyName = resolveNameClashes(usedNames, modelName)

    val results = mapNotNull { it.walk(isRoot = false, parentType) }

    val property = FieldSpec.builder(ParameterizedTypeName.get(JavaClassNames.List, JavaClassNames.CompiledSelection), propertyName)
        .initializer(results.map { it.initializer }.toListInitializerCodeblock(withNewLines = true))
        .addModifiers(Modifier.STATIC)
        .addModifiers(if (isRoot) Modifier.PUBLIC else Modifier.PRIVATE)
        .build()

    return results.flatMap { it.nestedFieldSpecs } + property
  }

  class SelectionResult(val initializer: CodeBlock, val nestedFieldSpecs: List<FieldSpec>)

  private fun GQLSelection.walk(isRoot: Boolean, parentType: String): SelectionResult? {
    return when (this) {
      is GQLField -> this.walk(isRoot, parentType)
      is GQLInlineFragment -> walk(isRoot)
      is GQLFragmentSpread -> walk()
    }
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

  private fun GQLField.walk(isRoot: Boolean, parentType: String): SelectionResult? {
    val expression = directives.toIncludeBooleanExpression()
    if (expression == BooleanExpression.False) {
      return null
    }

    val builder = CodeBlock.builder()

    val fieldDefinition = definitionFromScope(schema, parentType)!!

    builder.add("new $T($S, $L)", JavaClassNames.CompiledFieldBuilder, name, fieldDefinition.type.codeBlock())
    builder.indent()

    if (alias != null) {
      builder.add(".alias($S)", alias)
    }

    if (expression != BooleanExpression.True) {
      builder.add(".condition($L)", expression.toCompiledConditionInitializer())
    }
    if (arguments?.arguments?.isNotEmpty() == true) {
      builder.add(".arguments($L)", arguments!!.arguments.codeBlock(name, parentType))
    }

    var nestededFieldSpecs: List<FieldSpec> = emptyList()
    val selections = selectionSet?.selections ?: emptyList()
    if (selections.isNotEmpty()) {
      nestededFieldSpecs = selections.walk(alias ?: name, isRoot, fieldDefinition.type.leafType().name)
      builder.add(".selections($L)", nestededFieldSpecs.last().name)
    }
    builder.unindent()
    builder.add(".build()")

    return SelectionResult(builder.build(), nestededFieldSpecs)
  }

  private fun GQLInlineFragment.walk(isRoot: Boolean): SelectionResult? {
    val expression = directives.toIncludeBooleanExpression()
    if (expression == BooleanExpression.False) {
      return null
    }

    val name = "on${typeCondition.name.capitalizeFirstLetter()}"

    val builder = CodeBlock.builder()
    builder.add(
        "new $T($S, $L)",
        JavaClassNames.CompiledFragmentBuilder,
        typeCondition.name,
        possibleTypesCodeBlock(typeCondition.name)
    )
    builder.indent()
    if (expression !is BooleanExpression.True) {
      builder.add(".condition($L)", expression.toCompiledConditionInitializer())
    }

    var nestededFieldSpecs: List<FieldSpec> = emptyList()
    val selections = selectionSet.selections
    if (selections.isNotEmpty()) {
      nestededFieldSpecs = selections.walk(name, isRoot, typeCondition.name)
      builder.add(".selections($L)", nestededFieldSpecs.last().name)
    }
    builder.unindent()
    builder.add(".build()")

    return SelectionResult(builder.build(), nestededFieldSpecs)
  }

  private fun GQLFragmentSpread.walk(): SelectionResult? {
    val expression = directives.toIncludeBooleanExpression()
    if (expression == BooleanExpression.False) {
      return null
    }
    val fragmentDefinition = allFragmentDefinitions[name]!!

    val builder = CodeBlock.builder()
    builder.add(
        "new $T($S, $L)",
        JavaClassNames.CompiledFragmentBuilder,
        fragmentDefinition.typeCondition.name,
        possibleTypesCodeBlock(fragmentDefinition.typeCondition.name)
    )
    builder.indent()
    if (expression !is BooleanExpression.True) {
      builder.add(".condition($L)", expression.toCompiledConditionInitializer())
    }
    builder.add(".selections($T.$root)", context.resolver.resolveFragmentSelections(name))
    builder.unindent()
    builder.add(".build()")

    return SelectionResult(builder.build(), emptyList())
  }

  private fun possibleTypesCodeBlock(typeCondition: String): CodeBlock {
    return schema.possibleTypes(typeCondition).map { CodeBlock.of(S, it) }.toListInitializerCodeblock()
  }

  private fun GQLType.codeBlock(): CodeBlock {
    return when (this) {
      is GQLNonNullType -> {
        CodeBlock.of("new $T($L)", JavaClassNames.CompiledNotNullType, type.codeBlock())
      }
      is GQLListType -> {
        CodeBlock.of("new $T($L)", JavaClassNames.CompiledListType, type.codeBlock())
      }
      is GQLNamedType -> {
        context.resolver.resolveCompiledType(name)
      }
    }
  }

  private fun GQLListValue.codeBlock(): CodeBlock {
    return values.map {
      it.codeBlock()
    }.toListInitializerCodeblock()

  }

  private fun GQLObjectValue.codeBlock(): CodeBlock {
    return fields.map {
      it.name to it.value.codeBlock()
    }.toMapInitializerCodeblock()
  }

  private fun GQLValue.codeBlock(): CodeBlock {
    return when (this) {
      is GQLObjectValue -> codeBlock()
      is GQLListValue -> codeBlock()
      is GQLEnumValue -> CodeBlock.of(S, value) // FIXME
      is GQLIntValue -> CodeBlock.of(L, value)
      is GQLFloatValue -> CodeBlock.of(L, value)
      is GQLBooleanValue -> CodeBlock.of(L, value)
      is GQLStringValue -> CodeBlock.of(S, value)
      is GQLVariableValue -> CodeBlock.of("new $T($S)", JavaClassNames.CompiledVariable, name)
      is GQLNullValue -> CodeBlock.of("null")
    }
  }

  private fun List<GQLArgument>.codeBlock(fieldName: String, parentType: String): CodeBlock {
    val typeDefinition = schema.typeDefinition(parentType)
    val keyArgs = typeDefinition.keyArgs(fieldName, schema)
    val paginationArgs = typeDefinition.paginationArgs(fieldName, schema)

    val arguments = sortedBy { it.name }.map {
      val argumentBuilder = CodeBlock.builder()
      argumentBuilder.add(
          "new $T($S, $L, $L, $L)",
          JavaClassNames.CompiledArgument,
          it.name,
          it.value.codeBlock(),
          if (keyArgs.contains(it.name)) "true" else "false",
          if (paginationArgs.contains(it.name)) "true" else "false",
      )
      argumentBuilder.build()
    }
    return arguments.toListInitializerCodeblock()
  }
}

