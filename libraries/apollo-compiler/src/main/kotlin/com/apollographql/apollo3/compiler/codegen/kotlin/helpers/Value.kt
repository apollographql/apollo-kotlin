package com.apollographql.apollo3.compiler.codegen.kotlin.helpers

import com.apollographql.apollo3.compiler.codegen.kotlin.KotlinSymbols
import com.apollographql.apollo3.compiler.ir.IrBooleanValue
import com.apollographql.apollo3.compiler.ir.IrEnumValue
import com.apollographql.apollo3.compiler.ir.IrFloatValue
import com.apollographql.apollo3.compiler.ir.IrIntValue
import com.apollographql.apollo3.compiler.ir.IrListValue
import com.apollographql.apollo3.compiler.ir.IrNullValue
import com.apollographql.apollo3.compiler.ir.IrNumberValue
import com.apollographql.apollo3.compiler.ir.IrObjectValue
import com.apollographql.apollo3.compiler.ir.IrStringValue
import com.apollographql.apollo3.compiler.ir.IrValue
import com.apollographql.apollo3.compiler.ir.IrVariableValue
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

internal fun IrValue.codeBlock(): CodeBlock {
  return when (this) {
    is IrBooleanValue -> CodeBlock.of("%L", value)
    is IrEnumValue -> CodeBlock.of("%S", value) // FIXME
    is IrFloatValue -> CodeBlock.of("%L", value)
    is IrIntValue -> CodeBlock.of("%L", value)
    is IrListValue -> codeBlock()
    IrNullValue -> CodeBlock.of("null")
    is IrObjectValue -> codeBlock()
    is IrStringValue -> CodeBlock.of("%S", value)
    is IrVariableValue -> CodeBlock.of("%T(%S)", KotlinSymbols.CompiledVariable, name)
    is IrNumberValue -> CodeBlock.of("%L", value)
  }
}
