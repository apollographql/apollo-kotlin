package com.apollographql.apollo3.compiler.codegen.java.helpers

import com.apollographql.apollo3.compiler.codegen.java.JavaClassNames
import com.apollographql.apollo3.compiler.codegen.java.L
import com.apollographql.apollo3.compiler.codegen.java.S
import com.apollographql.apollo3.compiler.codegen.java.T
import com.apollographql.apollo3.compiler.ir.IrBooleanValue
import com.apollographql.apollo3.compiler.ir.IrEnumValue
import com.apollographql.apollo3.compiler.ir.IrFloatValue
import com.apollographql.apollo3.compiler.ir.IrIntValue
import com.apollographql.apollo3.compiler.ir.IrListValue
import com.apollographql.apollo3.compiler.ir.IrNullValue
import com.apollographql.apollo3.compiler.ir.IrObjectValue
import com.apollographql.apollo3.compiler.ir.IrStringValue
import com.apollographql.apollo3.compiler.ir.IrValue
import com.apollographql.apollo3.compiler.ir.IrVariableValue
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

internal fun IrValue.codeBlock(): CodeBlock {
  return when (this) {
    is IrObjectValue -> codeBlock()
    is IrListValue -> codeBlock()
    is IrEnumValue -> CodeBlock.of(S, value) // FIXME
    is IrIntValue -> CodeBlock.of(L, value)
    is IrFloatValue -> CodeBlock.of(L, value)
    is IrBooleanValue -> CodeBlock.of(L, value)
    is IrStringValue -> CodeBlock.of(S, value)
    is IrVariableValue -> CodeBlock.of("new $T($S)", JavaClassNames.CompiledVariable, name)
    is IrNullValue -> CodeBlock.of("null")
  }
}