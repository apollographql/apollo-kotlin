package com.apollographql.apollo3.compiler.codegen.selections

import com.apollographql.apollo3.api.BTerm
import com.apollographql.apollo3.api.BVariable
import com.apollographql.apollo3.api.BooleanExpression
import com.apollographql.apollo3.api.CompiledCompoundType
import com.apollographql.apollo3.api.CompiledCondition
import com.apollographql.apollo3.api.CompiledField
import com.apollographql.apollo3.api.CompiledFragment
import com.apollographql.apollo3.api.CompiledNamedType
import com.apollographql.apollo3.api.CompiledOtherType
import com.apollographql.apollo3.api.CompiledSelection
import com.apollographql.apollo3.api.MergedField
import com.apollographql.apollo3.api.Variable
import com.apollographql.apollo3.api.not
import com.apollographql.apollo3.ast.GQLArgument
import com.apollographql.apollo3.ast.GQLBooleanValue
import com.apollographql.apollo3.ast.GQLDirective
import com.apollographql.apollo3.ast.GQLEnumValue
import com.apollographql.apollo3.ast.GQLField
import com.apollographql.apollo3.ast.GQLFloatValue
import com.apollographql.apollo3.ast.GQLFragmentDefinition
import com.apollographql.apollo3.ast.GQLFragmentSpread
import com.apollographql.apollo3.ast.GQLInlineFragment
import com.apollographql.apollo3.ast.GQLIntValue
import com.apollographql.apollo3.ast.GQLInterfaceTypeDefinition
import com.apollographql.apollo3.ast.GQLListType
import com.apollographql.apollo3.ast.GQLListValue
import com.apollographql.apollo3.ast.GQLNamedType
import com.apollographql.apollo3.ast.GQLNonNullType
import com.apollographql.apollo3.ast.GQLNullValue
import com.apollographql.apollo3.ast.GQLObjectTypeDefinition
import com.apollographql.apollo3.ast.GQLObjectValue
import com.apollographql.apollo3.ast.GQLSelection
import com.apollographql.apollo3.ast.GQLStringValue
import com.apollographql.apollo3.ast.GQLType
import com.apollographql.apollo3.ast.GQLUnionTypeDefinition
import com.apollographql.apollo3.ast.GQLValue
import com.apollographql.apollo3.ast.GQLVariableValue
import com.apollographql.apollo3.ast.Schema
import com.apollographql.apollo3.ast.definitionFromScope
import com.apollographql.apollo3.ast.leafType
import com.apollographql.apollo3.compiler.applyIf
import com.apollographql.apollo3.compiler.capitalizeFirstLetter
import com.apollographql.apollo3.compiler.codegen.CgContext
import com.apollographql.apollo3.compiler.codegen.helpers.codeBlock
import com.apollographql.apollo3.compiler.ir.toBooleanExpression
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.asTypeName
import com.squareup.kotlinpoet.joinToCode

class CompiledSelectionsBuilder(
    private val context: CgContext,
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
        .addProperties(selections.walk(context.layout.rootSelectionsPropertyName(), false, parentType))
        .build()
  }

  private fun List<GQLSelection>.walk(name: String, private: Boolean, parentType: String): List<PropertySpec> {
    val propertyName = resolveNameClashes(usedNames, context.layout.propertyName(name))

    val results = mapNotNull { it.walk(true, parentType) }
    val builder = CodeBlock.builder()
    builder.add("listOf(\n")
    builder.indent()
    builder.add(results.map { it.initializer }.joinToCode(separator = ",\n", suffix = "\n"))
    builder.unindent()
    builder.add(")")

    val property = PropertySpec.builder(propertyName, List::class.parameterizedBy(CompiledSelection::class))
        .initializer(builder.build())
        .applyIf(private) {
          addModifiers(KModifier.PRIVATE)
        }
        .build()

    return results.flatMap { it.nestedPropertySpecs } + property
  }

  class SelectionResult(val initializer: CodeBlock, val nestedPropertySpecs: List<PropertySpec>)

  private fun GQLSelection.walk(private: Boolean, parentType: String): SelectionResult? {
    return when (this) {
      is GQLField -> this.walk(private, parentType)
      is GQLInlineFragment -> walk(private)
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

    return CodeBlock.of("%T(%S,·%L)", CompiledCondition::class.asTypeName(), expression.value.name, inverted.toString())
  }

  private fun GQLField.walk(private: Boolean, parentType: String): SelectionResult? {
    val expression = directives.toBooleanExpression()
    if (expression == BooleanExpression.False) {
      return null
    }

    val parameters = mutableListOf<CodeBlock>()
    parameters.add(CodeBlock.of("name·=·%S", name))
    if (alias != null) {
      parameters.add(CodeBlock.of("alias·=·%S", alias))
    }

    val typeDefinition = definitionFromScope(schema, parentType)!!
    parameters.add(
        CodeBlock.of(
            "type·=·%L",
            typeDefinition.type.codeBlock()
        )
    )
    if (expression != BooleanExpression.True) {
      parameters.add(
          CodeBlock.of(
              "condition·=·%L",
              expression.toCompiledConditionInitializer()
          )
      )
    }
    if (arguments?.arguments?.isNotEmpty() == true) {
      parameters.add(
          CodeBlock.of(
              "arguments·=·%L",
              arguments!!.arguments.codeBlock()
          )
      )
    }

    var nestededPropertySpecs: List<PropertySpec> = emptyList()
    val selections = selectionSet?.selections ?: emptyList()
    if (selections.isNotEmpty()) {
      nestededPropertySpecs = selections.walk(alias ?: name, private, typeDefinition.type.leafType().name)
      parameters.add(CodeBlock.of("selections·=·%L", nestededPropertySpecs.last().name))
    }

    val builder = CodeBlock.builder()
    builder.add("%T(\n", CompiledField::class)
    builder.indent()
    builder.add(parameters.joinToCode(separator = ",\n", suffix = "\n"))
    builder.unindent()
    builder.add(")")

    return SelectionResult(builder.build(), nestededPropertySpecs)
  }

  private fun GQLInlineFragment.walk(private: Boolean): SelectionResult? {
    val expression = directives.toBooleanExpression()
    if (expression == BooleanExpression.False) {
      return null
    }

    val parameters = mutableListOf<CodeBlock>()
    val name = "on${typeCondition.name.capitalizeFirstLetter()}"
    parameters.add(CodeBlock.of("possibleTypes·=·%L", possibleTypesCodeBlock(typeCondition.name)))
    if (expression !is BooleanExpression.True) {
      parameters.add(
          CodeBlock.of(
              "condition·=·%L",
              expression.toCompiledConditionInitializer()
          )
      )
    }

    var nestededPropertySpecs: List<PropertySpec> = emptyList()
    val selections = selectionSet.selections
    if (selections.isNotEmpty()) {
      nestededPropertySpecs = selections.walk(name, private, typeCondition.name)
      parameters.add(CodeBlock.of("selections·=·%L", nestededPropertySpecs.last().name))
    }

    val builder = CodeBlock.builder()
    builder.add("%T(\n", CompiledFragment::class.asTypeName())
    builder.indent()
    builder.add(parameters.joinToCode(separator = ",\n", suffix = "\n"))
    builder.unindent()
    builder.add(")")

    return SelectionResult(builder.build(), nestededPropertySpecs)
  }

  private fun GQLFragmentSpread.walk(): SelectionResult? {
    val expression = directives.toBooleanExpression()
    if (expression == BooleanExpression.False) {
      return null
    }

    val parameters = mutableListOf<CodeBlock>()
    if (expression !is BooleanExpression.True) {
      parameters.add(
          CodeBlock.of(
              "condition·=·%L",
              expression.toCompiledConditionInitializer()
          )
      )
    }

    val fragmentDefinition = allFragmentDefinitions[name]!!
    parameters.add(CodeBlock.of("possibleTypes·=·%L", possibleTypesCodeBlock(fragmentDefinition.typeCondition.name)))
    parameters.add(CodeBlock.of("selections·=·%T.%L", context.resolver.resolveFragmentSelections(name), context.layout.rootSelectionsPropertyName()))

    val builder = CodeBlock.builder()
    builder.add("%T(\n", CompiledFragment::class.asTypeName())
    builder.indent()
    builder.add(parameters.joinToCode(separator = ",\n", suffix = "\n"))
    builder.unindent()
    builder.add(")")

    return SelectionResult(builder.build(), emptyList())
  }

  private fun possibleTypesCodeBlock(typeCondition: String): CodeBlock {
    val builder = CodeBlock.builder()
    builder.add("listOf(")
    builder.add("%L", schema.possibleTypes(typeCondition).map { CodeBlock.of("%S", it) }.joinToCode(", "))
    builder.add(")")
    return builder.build()
  }

  private fun GQLType.codeBlock(): CodeBlock {
    return when (this) {
      is GQLNonNullType -> {
        val notNullFun = MemberName("com.apollographql.apollo3.api", "notNull")
        CodeBlock.of("%L.%M()", type.codeBlock(), notNullFun)
      }
      is GQLListType -> {
        val listFun = MemberName("com.apollographql.apollo3.api", "list")
        CodeBlock.of("%L.%M()", type.codeBlock(), listFun)
      }
      is GQLNamedType -> {
        val typeDefinition = schema.typeDefinition(name)

        when (typeDefinition) {
          is GQLUnionTypeDefinition,
          is GQLInterfaceTypeDefinition,
          is GQLObjectTypeDefinition,
          -> CodeBlock.of("%T(%S)", CompiledCompoundType::class, "unused")
          else
          -> CodeBlock.of("%T(%S)", CompiledOtherType::class, "unused")
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
      is GQLVariableValue -> CodeBlock.of("%T(%S)", Variable::class, name)
      is GQLNullValue -> CodeBlock.of("null")
    }
  }

  private fun List<GQLArgument>.codeBlock(): CodeBlock {
    if (isEmpty()) {
      return CodeBlock.of("emptyMap()")
    }

    val builder = CodeBlock.builder()
    builder.add("mapOf(")
    builder.indent()
    builder.add(
        map {
          CodeBlock.of("%S to %L", it.name, it.value.codeBlock())
        }.joinToCode(separator = ",\n", suffix = "\n")
    )
    builder.unindent()
    builder.add(")")
    return builder.build()
  }
}

