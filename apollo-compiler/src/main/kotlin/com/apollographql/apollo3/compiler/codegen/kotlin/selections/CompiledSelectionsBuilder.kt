package com.apollographql.apollo3.compiler.codegen.kotlin.selections

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
import com.apollographql.apollo3.compiler.applyIf
import com.apollographql.apollo3.compiler.capitalizeFirstLetter
import com.apollographql.apollo3.compiler.codegen.Identifier.root
import com.apollographql.apollo3.compiler.codegen.keyArgs
import com.apollographql.apollo3.compiler.codegen.kotlin.KotlinContext
import com.apollographql.apollo3.compiler.codegen.kotlin.KotlinSymbols
import com.apollographql.apollo3.compiler.codegen.paginationArgs
import com.apollographql.apollo3.compiler.ir.toIncludeBooleanExpression
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.joinToCode

class CompiledSelectionsBuilder(
    private val context: KotlinContext,
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
    return TypeSpec.objectBuilder(rootName)
        .addProperties(selections.walk(root, isRoot = true, parentType))
        .build()
  }

  private fun List<GQLSelection>.walk(name: String, isRoot: Boolean, parentType: String): List<PropertySpec> {
    val modelName = if (isRoot) root else context.layout.compiledSelectionsName(name)
    val propertyName = resolveNameClashes(usedNames, modelName)

    val results = mapNotNull { it.walk(isRoot = false, parentType) }
    val builder = CodeBlock.builder()
    builder.add("listOf(\n")
    builder.indent()
    builder.add(results.map { it.initializer }.joinToCode(separator = ",\n", suffix = "\n"))
    builder.unindent()
    builder.add(")")

    val property = PropertySpec.builder(propertyName, KotlinSymbols.List.parameterizedBy(KotlinSymbols.CompiledSelection))
        .initializer(builder.build())
        .applyIf(!isRoot) {
          addModifiers(KModifier.PRIVATE)
        }
        .build()

    return results.flatMap { it.nestedPropertySpecs } + property
  }

  class SelectionResult(val initializer: CodeBlock, val nestedPropertySpecs: List<PropertySpec>)

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

  private fun GQLField.walk(isRoot: Boolean, parentType: String): SelectionResult? {
    val expression = directives.toIncludeBooleanExpression()
    if (expression == BooleanExpression.False) {
      return null
    }


    val builder = CodeBlock.builder()
    builder.add("%T(\n", KotlinSymbols.CompiledFieldBuilder)
    builder.indent()
    builder.add("name·=·%S,\n", name)
    val fieldDefinition = definitionFromScope(schema, parentType)!!
    builder.add(
        CodeBlock.of("type·=·%L\n", fieldDefinition.type.codeBlock(context))
    )
    builder.unindent()
    builder.add(")")
    if (alias != null) {
      builder.add(".alias(%S)\n", alias)
    }

    if (expression != BooleanExpression.True) {
      builder.add(".condition(%L)\n", expression.toCompiledConditionInitializer())
    }
    if (arguments?.arguments?.isNotEmpty() == true) {
      builder.add(".arguments(%L)\n", arguments!!.arguments.codeBlock(name, parentType))
    }

    var nestededPropertySpecs: List<PropertySpec> = emptyList()
    val selections = selectionSet?.selections ?: emptyList()
    if (selections.isNotEmpty()) {
      nestededPropertySpecs = selections.walk(alias ?: name, isRoot, fieldDefinition.type.leafType().name)
      builder.add(".selections(%N)\n", nestededPropertySpecs.last().name)
    }
    builder.add(".build()")

    return SelectionResult(builder.build(), nestededPropertySpecs)
  }

  private fun GQLInlineFragment.walk(isRoot: Boolean): SelectionResult? {
    val expression = directives.toIncludeBooleanExpression()
    if (expression == BooleanExpression.False) {
      return null
    }

    val builder = CodeBlock.builder()
    builder.add("%T(\n", KotlinSymbols.CompiledFragmentBuilder)
    builder.indent()
    builder.add("typeCondition·=·%S,\n", typeCondition.name)
    builder.add("possibleTypes·=·%L\n", possibleTypesCodeBlock(typeCondition.name))
    builder.unindent()
    builder.add(")")
    if (expression !is BooleanExpression.True) {
      builder.add(".condition(%L)\n", expression.toCompiledConditionInitializer())
    }

    var nestedPropertySpecs: List<PropertySpec> = emptyList()
    val selections = selectionSet.selections
    if (selections.isNotEmpty()) {
      val name = "on${typeCondition.name.capitalizeFirstLetter()}"
      nestedPropertySpecs = selections.walk(name, isRoot, typeCondition.name)
      builder.add(".selections(%N)\n", nestedPropertySpecs.last().name)
    }
    builder.add(".build()")

    return SelectionResult(builder.build(), nestedPropertySpecs)
  }

  private fun GQLFragmentSpread.walk(): SelectionResult? {
    val expression = directives.toIncludeBooleanExpression()
    if (expression == BooleanExpression.False) {
      return null
    }

    val builder = CodeBlock.builder()
    builder.add("%T(\n", KotlinSymbols.CompiledFragmentBuilder)
    builder.indent()
    val fragmentDefinition = allFragmentDefinitions[name]!!
    builder.add("typeCondition·=·%S,\n", fragmentDefinition.typeCondition.name)
    builder.add("possibleTypes·=·(%L)\n", possibleTypesCodeBlock(fragmentDefinition.typeCondition.name))
    builder.unindent()
    builder.add(")")
    builder.add(".selections(%T.$root)\n", context.resolver.resolveFragmentSelections(name))
    if (expression !is BooleanExpression.True) {
      builder.add(".condition(%L)\n", expression.toCompiledConditionInitializer())
    }
    builder.add(".build()")
    return SelectionResult(builder.build(), emptyList())
  }

  private fun possibleTypesCodeBlock(typeCondition: String): CodeBlock {
    val builder = CodeBlock.builder()
    builder.add("listOf(")
    builder.add("%L", schema.possibleTypes(typeCondition).map { CodeBlock.of("%S", it) }.joinToCode(", "))
    builder.add(")")
    return builder.build()
  }

  companion object {
    fun GQLType.codeBlock(context: KotlinContext): CodeBlock {
      return when (this) {
        is GQLNonNullType -> {
          val notNullFun = MemberName("com.apollographql.apollo3.api", "notNull")
          CodeBlock.of("%L.%M()", type.codeBlock(context), notNullFun)
        }
        is GQLListType -> {
          val listFun = MemberName("com.apollographql.apollo3.api", "list")
          CodeBlock.of("%L.%M()", type.codeBlock(context), listFun)
        }
        is GQLNamedType -> {
          context.resolver.resolveCompiledType(name)
        }
      }
    }
  }


  private fun GQLListValue.codeBlock(): CodeBlock {
    if (values.isEmpty()) {
      // TODO: Is Nothing correct here?
      return CodeBlock.of("emptyList<Nothing>()")
    }

    return CodeBlock.builder().apply {
      add("listOf(\n")
      indent()
      values.forEach {
        add("%L,\n", it.codeBlock())
      }
      unindent()
      add(")")
    }.build()
  }

  private fun GQLObjectValue.codeBlock(): CodeBlock {
    if (fields.isEmpty()) {
      // TODO: Is Nothing correct here?
      return CodeBlock.of("emptyMap<Nothing, Nothing>()")
    }

    return CodeBlock.builder().apply {
      add("mapOf(\n")
      indent()
      fields.forEach {
        add("%S to %L,\n", it.name, it.value.codeBlock())
      }
      unindent()
      add(")")
    }.build()
  }

  private fun GQLValue.codeBlock(): CodeBlock {
    return when (this) {
      is GQLObjectValue -> codeBlock()
      is GQLListValue -> codeBlock()
      is GQLEnumValue -> CodeBlock.of("%S", value) // FIXME
      is GQLIntValue -> CodeBlock.of("%L", value)
      is GQLFloatValue -> CodeBlock.of("%L", value)
      is GQLBooleanValue -> CodeBlock.of("%L", value)
      is GQLStringValue -> CodeBlock.of("%S", value)
      is GQLVariableValue -> CodeBlock.of("%T(%S)", KotlinSymbols.CompiledVariable, name)
      is GQLNullValue -> CodeBlock.of("null")
    }
  }

  private fun List<GQLArgument>.codeBlock(fieldName: String, parentType: String): CodeBlock {
    if (isEmpty()) {
      return CodeBlock.of("emptyList()")
    }

    val typeDefinition = schema.typeDefinition(parentType)
    val keyArgs = typeDefinition.keyArgs(fieldName, schema)
    val paginationArgs = typeDefinition.paginationArgs(fieldName, schema)

    val builder = CodeBlock.builder()
    builder.add("listOf(\n")
    builder.indent()
    val arguments = sortedBy { it.name }.map {
      val argumentBuilder = CodeBlock.builder()
      argumentBuilder.add(
          "%T(%S,·%L",
          KotlinSymbols.CompiledArgument,
          it.name,
          it.value.codeBlock()
      )

      if (keyArgs.contains(it.name)) {
        argumentBuilder.add(",·isKey·=·true")
      }
      if (paginationArgs.contains(it.name)) {
        argumentBuilder.add(",·isPagination·=·true")
      }
      argumentBuilder.add(")")
      argumentBuilder.build()
    }
    builder.add("%L", arguments.joinToCode(",\n"))
    builder.add("\n")
    builder.unindent()
    builder.add(")")
    return builder.build()
  }
}

