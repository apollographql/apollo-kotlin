package com.apollographql.apollo3.compiler.ir

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable


@Serializable
internal sealed class IrValue
@Serializable
@SerialName("int")
internal data class IrIntValue(val value: Int) : IrValue()
@Serializable
@SerialName("float")
internal data class IrFloatValue(val value: Double) : IrValue()
@Serializable
@SerialName("string")
internal data class IrStringValue(val value: String) : IrValue()
@Serializable
@SerialName("boolean")
internal data class IrBooleanValue(val value: Boolean) : IrValue()
@Serializable
@SerialName("enum")
internal data class IrEnumValue(val value: String) : IrValue()
@Serializable
@SerialName("null")
internal object IrNullValue : IrValue()
@Serializable
@SerialName("object")
internal data class IrObjectValue(val fields: List<Field>) : IrValue() {
  @Serializable
  data class Field(val name: String, val value: IrValue)
}
@Serializable
@SerialName("list")
internal data class IrListValue(val values: List<IrValue>) : IrValue()
@Serializable
@SerialName("variable")
internal data class IrVariableValue(val name: String) : IrValue()