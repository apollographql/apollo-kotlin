package com.apollographql.apollo3.compiler.ir


internal sealed class IrValue

internal data class IrIntValue(val value: Int) : IrValue()
internal data class IrFloatValue(val value: Double) : IrValue()
internal data class IrStringValue(val value: String) : IrValue()
internal data class IrBooleanValue(val value: Boolean) : IrValue()
internal data class IrEnumValue(val value: String) : IrValue()
internal object IrNullValue : IrValue()
internal data class IrObjectValue(val fields: List<Field>) : IrValue() {
  data class Field(val name: String, val value: IrValue)
}

internal data class IrListValue(val values: List<IrValue>) : IrValue()
internal data class IrVariableValue(val name: String) : IrValue()