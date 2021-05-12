package com.apollographql.apollo3.compiler.codegen.compiledfield

import com.apollographql.apollo3.api.BVariable
import com.apollographql.apollo3.api.BooleanExpression
import com.apollographql.apollo3.api.FieldSet
import com.apollographql.apollo3.api.MergedField
import com.apollographql.apollo3.api.Variable
import com.apollographql.apollo3.compiler.codegen.CgLayout.Companion.modelName
import com.apollographql.apollo3.compiler.codegen.Identifier
import com.apollographql.apollo3.compiler.codegen.helpers.codeBlock
import com.apollographql.apollo3.compiler.ir.IrBooleanValue
import com.apollographql.apollo3.compiler.ir.IrCompiledArgument
import com.apollographql.apollo3.compiler.ir.IrCompiledField
import com.apollographql.apollo3.compiler.ir.IrCompiledFieldSet
import com.apollographql.apollo3.compiler.ir.IrCompiledType
import com.apollographql.apollo3.compiler.ir.IrEnumValue
import com.apollographql.apollo3.compiler.ir.IrFloatValue
import com.apollographql.apollo3.compiler.ir.IrIntValue
import com.apollographql.apollo3.compiler.ir.IrListCompiledType
import com.apollographql.apollo3.compiler.ir.IrListValue
import com.apollographql.apollo3.compiler.ir.IrNamedCompiledType
import com.apollographql.apollo3.compiler.ir.IrNonNullCompiledType
import com.apollographql.apollo3.compiler.ir.IrNullValue
import com.apollographql.apollo3.compiler.ir.IrObjectValue
import com.apollographql.apollo3.compiler.ir.IrStringValue
import com.apollographql.apollo3.compiler.ir.IrValue
import com.apollographql.apollo3.compiler.ir.IrVariableValue
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.asTypeName
import com.squareup.kotlinpoet.joinToCode

class CompiledFieldsBuilder(
    private val rootCompiledField: IrCompiledField,
    private val rootName: String,
) {
  fun build(): TypeSpec {
    return fieldSetTypeSpec(rootName, listOf(rootCompiledField))
  }

  private fun objectName(compiledField: IrCompiledField, fieldSet: IrCompiledFieldSet): String {
    return modelName(compiledField, fieldSet.typeSet)
  }

  private fun IrCompiledField.possibleFieldSets() = fieldSets.filter {
    it.typeSet.size == 1 // the fallback field set (can contain possible types too)
        || it.possibleTypes.isNotEmpty()
  }

  private fun fieldSetsCodeBlock(compiledField: IrCompiledField): CodeBlock {
    return CodeBlock.builder().apply {
      addStatement("listOf(")
      indent()
      compiledField.possibleFieldSets().flatMap { fieldSet ->
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
            FieldSet::class,
            pair.first?.let { "\"$it\"" },
            // This doesn't use %M on purpose as fields will name clash
            "${objectName(compiledField, pair.second)}.fields"
        )
      }
      unindent()
      add(")")
    }.build()
  }

  private fun fieldSetTypeSpec(name: String, compiledFields: List<IrCompiledField>): TypeSpec {
    return TypeSpec.objectBuilder(name)
        .addProperty(responseFieldsPropertySpec(compiledFields))
        .addTypes(compiledFields.flatMap { field ->
          field.possibleFieldSets().map {
            fieldSetTypeSpec(
                objectName(field, it),
                it.compiledFields
            )
          }
        })
        .build()
  }

  private fun responseFieldsPropertySpec(compiledFields: List<IrCompiledField>): PropertySpec {
    return PropertySpec.builder(Identifier.fields, Array::class.parameterizedBy(MergedField::class))
        .initializer(responseFieldsCodeBlock(compiledFields))
        .build()
  }

  private fun responseFieldsCodeBlock(compiledFields: List<IrCompiledField>): CodeBlock {
    val builder = CodeBlock.builder()

    builder.addStatement("arrayOf(")
    builder.indent()

    compiledFields.forEach {
      builder.add("%L,\n", it.responseFieldsCodeBlock())
    }
    builder.unindent()
    builder.addStatement(")")

    return builder.build()
  }

  private fun IrCompiledType.codeBlock(): CodeBlock {
    return when (this) {
      is IrNonNullCompiledType -> {
        val notNullFun = MemberName("com.apollographql.apollo3.api", "notNull")
        CodeBlock.of("%L.%M()", ofType.codeBlock(), notNullFun)
      }
      is IrListCompiledType -> {
        val listFun = MemberName("com.apollographql.apollo3.api", "list")
        CodeBlock.of("%L.%M()", ofType.codeBlock(), listFun)
      }
      is IrNamedCompiledType -> {
        if (compound) {
          CodeBlock.of("%T(%S)", MergedField.Type.Named.Object::class, "unused")
        } else {
          CodeBlock.of("%T(%S)", MergedField.Type.Named.Other::class, "unused")
        }
      }
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

  private fun List<IrCompiledArgument>.codeBlock(): CodeBlock {
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

  private fun IrCompiledField.responseFieldsCodeBlock(): CodeBlock {
    if (name == "__typename" && alias == null) {
      return CodeBlock.of("%T.Typename", MergedField::class.asTypeName())
    }
    val builder = CodeBlock.builder().add("%T(\n", MergedField::class)
    builder.indent()
    builder.add("type = %L,\n", type.codeBlock())
    builder.add("fieldName = %S,\n", name)
    if (alias != null) {
      builder.add("responseName = %S,\n", alias)
    }
    if (arguments.isNotEmpty()) {
      builder.add("arguments = %L,\n", arguments.codeBlock())
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

