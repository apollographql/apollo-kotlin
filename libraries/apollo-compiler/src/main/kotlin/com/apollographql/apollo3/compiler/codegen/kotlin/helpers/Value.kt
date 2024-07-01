package com.apollographql.apollo.compiler.codegen.kotlin.helpers

import com.apollographql.apollo.compiler.codegen.kotlin.KotlinSymbols
import com.apollographql.apollo.compiler.ir.IrBooleanValue
import com.apollographql.apollo.compiler.ir.IrEnumValue
import com.apollographql.apollo.compiler.ir.IrFloatValue
import com.apollographql.apollo.compiler.ir.IrIntValue
import com.apollographql.apollo.compiler.ir.IrListValue
import com.apollographql.apollo.compiler.ir.IrNullValue
import com.apollographql.apollo.compiler.ir.IrObjectValue
import com.apollographql.apollo.compiler.ir.IrStringValue
import com.apollographql.apollo.compiler.ir.IrValue
import com.apollographql.apollo.compiler.ir.IrVariableValue
import com.squareup.kotlinpoet.CodeBlock

private fun IrListValue.codeBlock(): CodeBlock {
  if (values.isEmpty()) {
    // TODO: Is Nothing correct here?
    return CodeBlock.of("emptyList<Nothing>()")
  }

  return values.map { it.codeBlock() }.toListInitializerCodeblock(true)
}

private fun IrObjectValue.codeBlock(): CodeBlock {
  if (fields.isEmpty()) {
    // TODO: Is Nothing correct here?
    return CodeBlock.of("emptyMap<Nothing, Nothing>()")
  }

  return fields.map { it.name to it.value.codeBlock() }.toMapInitializerCodeblock(true)
}

/**
 * Converts an [IrValue] to its equivalent Kotlin expression as in `ApolloJsonElement`
 * One exception is variables which get mapped to `CompiledVariable`
 */
internal fun IrValue.codeBlock(): CodeBlock {
  return when (this) {
    is IrBooleanValue -> CodeBlock.of("%L", value)
    // Enums are serialized to JSON String
    is IrEnumValue -> CodeBlock.of("%S", value)
    is IrIntValue -> {
      val asInt = value.toIntOrNull()
      if (asInt != null) {
        // The value fits in kotlin Int
        CodeBlock.of("%L", value)
      } else {
        CodeBlock.of("%T(%S)", KotlinSymbols.JsonNumber, value)
      }
    }

    is IrFloatValue -> {
      val asDouble = value.toDoubleOrNull()
      if (asDouble != null) {
        // The value fits in a kotlin Double
        CodeBlock.of("%L", value)
      } else {
        CodeBlock.of("%T(%S)", KotlinSymbols.JsonNumber, value)
      }
    }

    is IrListValue -> codeBlock()
    IrNullValue -> CodeBlock.of("null")
    is IrObjectValue -> codeBlock()
    is IrStringValue -> CodeBlock.of("%S", value)
    is IrVariableValue -> CodeBlock.of("%T(%S)", KotlinSymbols.CompiledVariable, name)
  }
}
