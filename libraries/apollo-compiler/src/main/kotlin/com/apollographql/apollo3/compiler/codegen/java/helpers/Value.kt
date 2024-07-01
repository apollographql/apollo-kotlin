package com.apollographql.apollo.compiler.codegen.java.helpers

import com.apollographql.apollo.compiler.codegen.java.JavaClassNames
import com.apollographql.apollo.compiler.codegen.java.L
import com.apollographql.apollo.compiler.codegen.java.S
import com.apollographql.apollo.compiler.codegen.java.T
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
import com.squareup.javapoet.CodeBlock


private fun IrListValue.codeBlock(): CodeBlock {
  return values.map {
    it.codeBlock()
  }.toListInitializerCodeblock()

}

private fun IrObjectValue.codeBlock(): CodeBlock {
  return fields.map {
    it.name to it.value.codeBlock()
  }.toMapInitializerCodeblock()
}

/**
 * Converts an [IrValue] to its equivalent Kotlin expression as in `ApolloJsonElement`
 * One exception is variables which get mapped to `CompiledVariable`
 */
internal fun IrValue.codeBlock(): CodeBlock {
  return when (this) {
    is IrObjectValue -> codeBlock()
    is IrListValue -> codeBlock()
    // Enums are serialized to JSON String
    is IrEnumValue -> CodeBlock.of(S, value)
    is IrIntValue -> {
      val asInt = value.toIntOrNull()
      if (asInt != null) {
        // The value fits in kotlin Int
        CodeBlock.of(L, value)
      } else {
        CodeBlock.of("new $T($S)", JavaClassNames.JsonNumber, value)
      }
    }

    is IrFloatValue -> {
      val asDouble = value.toDoubleOrNull()
      if (asDouble != null) {
        // The value fits in a kotlin Double
        CodeBlock.of(L, value)
      } else {
        CodeBlock.of("new $T($S)", JavaClassNames.JsonNumber, value)
      }
    }

    is IrBooleanValue -> CodeBlock.of(L, value)
    is IrStringValue -> CodeBlock.of(S, value)
    is IrVariableValue -> CodeBlock.of("new $T($S)", JavaClassNames.CompiledVariable, name)
    is IrNullValue -> CodeBlock.of("null")
  }
}