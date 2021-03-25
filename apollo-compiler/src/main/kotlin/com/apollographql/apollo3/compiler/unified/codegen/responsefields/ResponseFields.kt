package com.apollographql.apollo3.compiler.unified.codegen

import com.apollographql.apollo3.api.ResponseField
import com.apollographql.apollo3.api.Variable
import com.apollographql.apollo3.compiler.backend.codegen.Identifier
import com.apollographql.apollo3.compiler.backend.codegen.kotlinNameForResponseFields
import com.apollographql.apollo3.compiler.backend.codegen.responseFieldsPackageName
import com.apollographql.apollo3.compiler.unified.BooleanExpression
import com.apollographql.apollo3.compiler.unified.IrArgument
import com.apollographql.apollo3.compiler.unified.IrBooleanValue
import com.apollographql.apollo3.compiler.unified.IrCompoundType
import com.apollographql.apollo3.compiler.unified.IrEnumValue
import com.apollographql.apollo3.compiler.unified.IrField
import com.apollographql.apollo3.compiler.unified.IrFieldSet
import com.apollographql.apollo3.compiler.unified.IrFloatValue
import com.apollographql.apollo3.compiler.unified.IrIntValue
import com.apollographql.apollo3.compiler.unified.IrListType
import com.apollographql.apollo3.compiler.unified.IrListValue
import com.apollographql.apollo3.compiler.unified.IrNonNullType
import com.apollographql.apollo3.compiler.unified.IrNullValue
import com.apollographql.apollo3.compiler.unified.IrObjectValue
import com.apollographql.apollo3.compiler.unified.IrStringValue
import com.apollographql.apollo3.compiler.unified.IrType
import com.apollographql.apollo3.compiler.unified.IrValue
import com.apollographql.apollo3.compiler.unified.IrVariableValue
import com.apollographql.apollo3.compiler.unified.codegen.helpers.typeName
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.asTypeName
import com.squareup.kotlinpoet.joinToCode

private fun IrFieldSet.containerTypeName(): ClassName {
  return fullPath.copy(
      packageName = responseFieldsPackageName(fullPath.packageName),
      elements = listOf(kotlinNameForResponseFields(fullPath.elements.first())) + fullPath.elements.drop(1)
  ).typeName()
}

internal fun dataResponseFieldsItemSpec(name: String, dataField: IrField): TypeSpec {
  return fieldSetTypeSpec(name, listOf(dataField))
}

private fun IrField.fieldSetsCodeBlock(): CodeBlock {
  return CodeBlock.builder().apply {
    addStatement("listOf(")
    indent()
    implementations.flatMap { fieldSet ->
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
          // This doesn't use %M on purpose as fields will nameclash 
          "${pair.second.fullPath.elements.last()}.fields"
      )
    }
    unindent()
    add(")")
  }.build()
}

private fun fieldSetTypeSpec(modelName: String, fields: List<IrField>): TypeSpec {
  return TypeSpec.objectBuilder(modelName)
      .addProperty(responseFieldsPropertySpec(fields))
      .addTypes(fields.flatMap {
        it.implementations.map {
          fieldSetTypeSpec(it.modelName, it.fields)
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
    is IrCompoundType -> CodeBlock.of("%T(%S)", ResponseField.Type.Named.Object::class, "unused")
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
  return when(this) {
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
  if (name == "__typename" && alias == null) {
    return CodeBlock.of("%T.Typename", ResponseField::class.asTypeName())
  }
  val builder = CodeBlock.builder().add("%T(\n", ResponseField::class)
  builder.indent()
  builder.add("type = %L,\n", type.codeBlock())
  builder.add("fieldName = %S,\n", name)
  if (responseName != name) {
    builder.add("responseName = %S,\n", responseName)
  }
  if (arguments.isNotEmpty()) {
    builder.add("arguments = %L,\n", arguments.codeBlock())
  }

  if (condition != BooleanExpression.True) {
    // TODO builder.add("conditions = %L,\n", conditionsListCode(conditions))
  }
  if (implementations.isNotEmpty()) {
    builder.add("fieldSets = %L,\n", fieldSetsCodeBlock())
  }
  builder.unindent()
  builder.add(")")

  return builder.build()
}