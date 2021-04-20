package com.apollographql.apollo3.compiler.unified.codegen.responsefields

import com.apollographql.apollo3.api.ResponseField
import com.apollographql.apollo3.api.Variable
import com.apollographql.apollo3.compiler.backend.codegen.Identifier
import com.apollographql.apollo3.compiler.unified.codegen.CgLayout.Companion.modelName
import com.apollographql.apollo3.api.BooleanExpression
import com.apollographql.apollo3.compiler.unified.ir.IrArgument
import com.apollographql.apollo3.compiler.unified.ir.IrBooleanValue
import com.apollographql.apollo3.compiler.unified.ir.IrEnumValue
import com.apollographql.apollo3.compiler.unified.ir.IrField
import com.apollographql.apollo3.compiler.unified.ir.IrFieldSet
import com.apollographql.apollo3.compiler.unified.ir.IrFloatValue
import com.apollographql.apollo3.compiler.unified.ir.IrIntValue
import com.apollographql.apollo3.compiler.unified.ir.IrListType
import com.apollographql.apollo3.compiler.unified.ir.IrListValue
import com.apollographql.apollo3.compiler.unified.ir.IrModelType
import com.apollographql.apollo3.compiler.unified.ir.IrNonNullType
import com.apollographql.apollo3.compiler.unified.ir.IrNullValue
import com.apollographql.apollo3.compiler.unified.ir.IrObjectValue
import com.apollographql.apollo3.compiler.unified.ir.IrStringValue
import com.apollographql.apollo3.compiler.unified.ir.IrType
import com.apollographql.apollo3.compiler.unified.ir.IrValue
import com.apollographql.apollo3.compiler.unified.ir.IrVariableValue
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.asTypeName
import com.squareup.kotlinpoet.joinToCode

class ResponseFieldsBuilder(
    private val rootField: IrField,
    private val rootName: String,
) {
  fun build(): TypeSpec {
    return fieldSetTypeSpec(rootName, listOf(rootField))
  }

  private fun objectName(field: IrField, fieldSet: IrFieldSet): String {
    return modelName(field.info, fieldSet.typeSet, false)
  }

  private fun IrField.possibleFieldSets() = fieldSets.filter {
    it.typeSet.size == 1 // the fallback field set (can contain possible types too)
        || it.possibleTypes.isNotEmpty()
  }

  private fun fieldSetsCodeBlock(field: IrField): CodeBlock {
    return CodeBlock.builder().apply {
      addStatement("listOf(")
      indent()
      field.possibleFieldSets().flatMap { fieldSet ->
        if (fieldSet.typeSet.size == 1) {
          listOf(null to fieldSet)
        } else {
          fieldSet.possibleTypes.map {
            it to fieldSet
          }
        }
      }.forEach { pair ->
        addStatement(
            "%T(%L, %L),",
            ResponseField.FieldSet::class,
            pair.first?.let { "\"$it\"" },
            // This doesn't use %M on purpose as fields will name clash
            "${objectName(field, pair.second)}.fields"
        )
      }
      unindent()
      add(")")
    }.build()
  }

  private fun fieldSetTypeSpec(name: String, fields: List<IrField>): TypeSpec {
    return TypeSpec.objectBuilder(name)
        .addProperty(responseFieldsPropertySpec(fields))
        .addTypes(fields.flatMap { field ->
          field.possibleFieldSets().map {
            fieldSetTypeSpec(
                objectName(field, it),
                it.fields
            )
          }
        })
        .build()
  }

  private fun responseFieldsPropertySpec(fields: List<IrField>): PropertySpec {
    return PropertySpec.builder(Identifier.fields, Array::class.parameterizedBy(ResponseField::class))
        .initializer(responseFieldsCodeBlock(fields))
        .build()
  }

  private fun responseFieldsCodeBlock(fields: List<IrField>): CodeBlock {
    val builder = CodeBlock.builder()

    builder.addStatement("arrayOf(")
    builder.indent()

    fields.forEach {
      builder.add("%L,\n", it.responseFieldsCodeBlock())
    }
    builder.unindent()
    builder.addStatement(")")

    return builder.build()
  }

  private fun IrType.codeBlock(): CodeBlock {
    return when (this) {
      is IrNonNullType -> {
        val notNullFun = MemberName("com.apollographql.apollo3.api", "notNull")
        CodeBlock.of("%L.%M()", ofType.codeBlock(), notNullFun)
      }
      is IrListType -> {
        val listFun = MemberName("com.apollographql.apollo3.api", "list")
        CodeBlock.of("%L.%M()", ofType.codeBlock(), listFun)
      }
      is IrModelType -> CodeBlock.of("%T(%S)", ResponseField.Type.Named.Object::class, "unused")
      else -> CodeBlock.of("%T(%S)", ResponseField.Type.Named.Other::class, "unused")
    }
  }

  private fun IrListValue.codeBlock(): CodeBlock {
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

  private fun IrObjectValue.codeBlock(): CodeBlock {
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

  private fun IrValue.codeBlock(): CodeBlock {
    return when (this) {
      is IrObjectValue -> codeBlock()
      is IrListValue -> codeBlock()
      is IrEnumValue -> CodeBlock.of("%S", value) // FIXME
      is IrIntValue -> CodeBlock.of("%L", value)
      is IrFloatValue -> CodeBlock.of("%L", value)
      is IrBooleanValue -> CodeBlock.of("%L", value)
      is IrStringValue -> CodeBlock.of("%S", value)
      is IrVariableValue -> CodeBlock.of("%T(%S)", Variable::class, name)
      is IrNullValue -> CodeBlock.of("null")
    }
  }

  private fun List<IrArgument>.codeBlock(): CodeBlock {
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

  private fun IrField.responseFieldsCodeBlock(): CodeBlock {
    if (info.name == "__typename" && info.alias == null) {
      return CodeBlock.of("%T.Typename", ResponseField::class.asTypeName())
    }
    val builder = CodeBlock.builder().add("%T(\n", ResponseField::class)
    builder.indent()
    builder.add("type = %L,\n", info.type.codeBlock())
    builder.add("fieldName = %S,\n", info.name)
    if (info.responseName != info.name) {
      builder.add("responseName = %S,\n", info.responseName)
    }
    if (info.arguments.isNotEmpty()) {
      builder.add("arguments = %L,\n", info.arguments.codeBlock())
    }

    if (condition != BooleanExpression.True) {
      builder.add("condition = %L,\n", condition.codeBlock())
    }
    if (possibleFieldSets().isNotEmpty()) {
      builder.add("fieldSets = %L,\n", fieldSetsCodeBlock(this))
    }
    builder.unindent()
    builder.add(")")

    return builder.build()
  }
}

private fun BooleanExpression.codeBlock(): CodeBlock {
  return when(this) {
    is BooleanExpression.False -> CodeBlock.of("%T", BooleanExpression.False::class.asTypeName())
    is BooleanExpression.True -> CodeBlock.of("%T", BooleanExpression.True::class.asTypeName())
    is BooleanExpression.And -> {
      val parameters = booleanExpressions.map {
        it.codeBlock()
      }.joinToCode(",")
      CodeBlock.of("%T(setOf(%L))", BooleanExpression.And::class.asTypeName(), parameters)
    }
    is BooleanExpression.Or -> {
      val parameters = booleanExpressions.map {
        it.codeBlock()
      }.joinToCode(",")
      CodeBlock.of("%T(setOf(%L))", BooleanExpression.Or::class.asTypeName(), parameters)
    }
    is BooleanExpression.Not -> CodeBlock.of("%T(%L)", BooleanExpression.Not::class.asTypeName(), booleanExpression.codeBlock())
    is BooleanExpression.Type -> CodeBlock.of("%T(%S)", BooleanExpression.Type::class.asTypeName(), name)
    is BooleanExpression.Variable -> CodeBlock.of("%T(%S)", BooleanExpression.Variable::class.asTypeName(), name)
  }
}