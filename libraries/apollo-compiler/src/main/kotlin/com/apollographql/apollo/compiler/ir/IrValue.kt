package com.apollographql.apollo.compiler.ir

import com.apollographql.apollo.annotations.ApolloExperimental
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@ApolloExperimental
sealed class IrValue
@Serializable
@SerialName("int")
@ApolloExperimental
data class IrIntValue(val value: String) : IrValue()
@Serializable
@SerialName("float")
@ApolloExperimental
data class IrFloatValue(val value: String) : IrValue()
@Serializable
@SerialName("string")
@ApolloExperimental
data class IrStringValue(val value: String) : IrValue()
@Serializable
@SerialName("boolean")
@ApolloExperimental
data class IrBooleanValue(val value: Boolean) : IrValue()
@Serializable
@SerialName("enum")
@ApolloExperimental
data class IrEnumValue(val value: String) : IrValue()
@Serializable
@SerialName("null")
@ApolloExperimental
object IrNullValue : IrValue()
@Serializable
@SerialName("object")
@ApolloExperimental
data class IrObjectValue(val fields: List<Field>) : IrValue() {
  @Serializable
  @ApolloExperimental
  data class Field(val name: String, val value: IrValue)
}
@Serializable
@SerialName("list")
internal data class IrListValue(val values: List<IrValue>) : IrValue()
@Serializable
@SerialName("variable")
internal data class IrVariableValue(val name: String) : IrValue()