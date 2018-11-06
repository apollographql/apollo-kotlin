package com.example.nested_inline_fragment.fragment

import com.apollographql.apollo.api.GraphqlFragment
import com.apollographql.apollo.api.ResponseField
import com.apollographql.apollo.api.ResponseFieldMarshaller
import com.apollographql.apollo.api.ResponseReader
import javax.annotation.Generated
import kotlin.Array
import kotlin.Boolean
import kotlin.String
import kotlin.Suppress
import kotlin.collections.List

@Generated("Apollo GraphQL")
data class TestSetting(
    val __typename: String,
    val value: Value1?,
    val asSelectSetting: AsSelectSetting?
) : GraphqlFragment {
    override fun marshaller(): ResponseFieldMarshaller = ResponseFieldMarshaller {
        it.writeString(RESPONSE_FIELDS[0], __typename)
        it.writeObject(RESPONSE_FIELDS[1], value?.marshaller())
        it.writeObject(RESPONSE_FIELDS[2], asSelectSetting?.marshaller())
    }

    companion object {
        private val RESPONSE_FIELDS: Array<ResponseField> = arrayOf(
                ResponseField.forString("__typename", "__typename", null, false, null),
                ResponseField.forObject("value", "value", null, true, null),
                ResponseField.forInlineFragment("__typename", "__typename", listOf("SelectSetting"))
                )

        val FRAGMENT_DEFINITION: String = """
                |fragment TestSetting on Setting {
                |  __typename
                |  value {
                |    __typename
                |    ... on StringListSettingValue {
                |      list
                |    }
                |  }
                |  ... on SelectSetting {
                |    options {
                |      __typename
                |      allowFreeText
                |      id
                |      label
                |    }
                |  }
                |}
                """.trimMargin()

        val POSSIBLE_TYPES: Array<String> =
                arrayOf("BooleanSetting", "SelectSetting", "StringListSetting", "TextSetting", "TypedStringListSetting")

        operator fun invoke(reader: ResponseReader): TestSetting {
            val __typename = reader.readString(RESPONSE_FIELDS[0])
            val value = reader.readObject<Value1>(RESPONSE_FIELDS[1]) {
                Value1(it)
            }

            val asSelectSetting = reader.readConditional(RESPONSE_FIELDS[2]) { conditionalType, reader ->
                AsSelectSetting(reader)
            }

            return TestSetting(
                __typename = __typename,
                value = value,
                asSelectSetting = asSelectSetting
            )
        }
    }

    data class AsStringListSettingValue(val __typename: String, val list: List<String>?) {
        fun marshaller(): ResponseFieldMarshaller = ResponseFieldMarshaller {
            it.writeString(RESPONSE_FIELDS[0], __typename)
            it.writeList(RESPONSE_FIELDS[1], list) { value, listItemWriter ->
                @Suppress("NAME_SHADOWING")
                value?.forEach { value ->
                    listItemWriter.writeString(value)
                }
            }
        }

        companion object {
            private val RESPONSE_FIELDS: Array<ResponseField> = arrayOf(
                    ResponseField.forString("__typename", "__typename", null, false, null),
                    ResponseField.forList("list", "list", null, true, null)
                    )

            operator fun invoke(reader: ResponseReader): AsStringListSettingValue {
                val __typename = reader.readString(RESPONSE_FIELDS[0])
                val list = reader.readList<String>(RESPONSE_FIELDS[1]) {
                    it.readString()
                }
                return AsStringListSettingValue(
                    __typename = __typename,
                    list = list
                )
            }
        }
    }

    data class Value(val __typename: String, val asStringListSettingValue: AsStringListSettingValue?) {
        fun marshaller(): ResponseFieldMarshaller = ResponseFieldMarshaller {
            it.writeString(RESPONSE_FIELDS[0], __typename)
            it.writeObject(RESPONSE_FIELDS[1], asStringListSettingValue?.marshaller())
        }

        companion object {
            private val RESPONSE_FIELDS: Array<ResponseField> = arrayOf(
                    ResponseField.forString("__typename", "__typename", null, false, null),
                    ResponseField.forInlineFragment("__typename", "__typename", listOf("StringListSettingValue"))
                    )

            operator fun invoke(reader: ResponseReader): Value {
                val __typename = reader.readString(RESPONSE_FIELDS[0])
                val asStringListSettingValue = reader.readConditional(RESPONSE_FIELDS[1]) { conditionalType, reader ->
                    AsStringListSettingValue(reader)
                }

                return Value(
                    __typename = __typename,
                    asStringListSettingValue = asStringListSettingValue
                )
            }
        }
    }

    data class Option(
        val __typename: String,
        val allowFreeText: Boolean,
        val id: String,
        val label: String
    ) {
        fun marshaller(): ResponseFieldMarshaller = ResponseFieldMarshaller {
            it.writeString(RESPONSE_FIELDS[0], __typename)
            it.writeBoolean(RESPONSE_FIELDS[1], allowFreeText)
            it.writeString(RESPONSE_FIELDS[2], id)
            it.writeString(RESPONSE_FIELDS[3], label)
        }

        companion object {
            private val RESPONSE_FIELDS: Array<ResponseField> = arrayOf(
                    ResponseField.forString("__typename", "__typename", null, false, null),
                    ResponseField.forBoolean("allowFreeText", "allowFreeText", null, false, null),
                    ResponseField.forString("id", "id", null, false, null),
                    ResponseField.forString("label", "label", null, false, null)
                    )

            operator fun invoke(reader: ResponseReader): Option {
                val __typename = reader.readString(RESPONSE_FIELDS[0])
                val allowFreeText = reader.readBoolean(RESPONSE_FIELDS[1])
                val id = reader.readString(RESPONSE_FIELDS[2])
                val label = reader.readString(RESPONSE_FIELDS[3])
                return Option(
                    __typename = __typename,
                    allowFreeText = allowFreeText,
                    id = id,
                    label = label
                )
            }
        }
    }

    data class AsSelectSetting(
        val __typename: String,
        val value: Value?,
        val options: List<Option>?
    ) {
        fun marshaller(): ResponseFieldMarshaller = ResponseFieldMarshaller {
            it.writeString(RESPONSE_FIELDS[0], __typename)
            it.writeObject(RESPONSE_FIELDS[1], value?.marshaller())
            it.writeList(RESPONSE_FIELDS[2], options) { value, listItemWriter ->
                @Suppress("NAME_SHADOWING")
                value?.forEach { value ->
                    listItemWriter.writeObject(value?.marshaller())
                }
            }
        }

        companion object {
            private val RESPONSE_FIELDS: Array<ResponseField> = arrayOf(
                    ResponseField.forString("__typename", "__typename", null, false, null),
                    ResponseField.forObject("value", "value", null, true, null),
                    ResponseField.forList("options", "options", null, true, null)
                    )

            operator fun invoke(reader: ResponseReader): AsSelectSetting {
                val __typename = reader.readString(RESPONSE_FIELDS[0])
                val value = reader.readObject<Value>(RESPONSE_FIELDS[1]) {
                    Value(it)
                }

                val options = reader.readList<Option>(RESPONSE_FIELDS[2]) {
                    it.readObject<Option> {
                        Option(it)
                    }

                }
                return AsSelectSetting(
                    __typename = __typename,
                    value = value,
                    options = options
                )
            }
        }
    }

    data class AsStringListSettingValue1(val __typename: String, val list: List<String>?) {
        fun marshaller(): ResponseFieldMarshaller = ResponseFieldMarshaller {
            it.writeString(RESPONSE_FIELDS[0], __typename)
            it.writeList(RESPONSE_FIELDS[1], list) { value, listItemWriter ->
                @Suppress("NAME_SHADOWING")
                value?.forEach { value ->
                    listItemWriter.writeString(value)
                }
            }
        }

        companion object {
            private val RESPONSE_FIELDS: Array<ResponseField> = arrayOf(
                    ResponseField.forString("__typename", "__typename", null, false, null),
                    ResponseField.forList("list", "list", null, true, null)
                    )

            operator fun invoke(reader: ResponseReader): AsStringListSettingValue1 {
                val __typename = reader.readString(RESPONSE_FIELDS[0])
                val list = reader.readList<String>(RESPONSE_FIELDS[1]) {
                    it.readString()
                }
                return AsStringListSettingValue1(
                    __typename = __typename,
                    list = list
                )
            }
        }
    }

    data class Value1(val __typename: String, val asStringListSettingValue1: AsStringListSettingValue1?) {
        fun marshaller(): ResponseFieldMarshaller = ResponseFieldMarshaller {
            it.writeString(RESPONSE_FIELDS[0], __typename)
            it.writeObject(RESPONSE_FIELDS[1], asStringListSettingValue1?.marshaller())
        }

        companion object {
            private val RESPONSE_FIELDS: Array<ResponseField> = arrayOf(
                    ResponseField.forString("__typename", "__typename", null, false, null),
                    ResponseField.forInlineFragment("__typename", "__typename", listOf("StringListSettingValue"))
                    )

            operator fun invoke(reader: ResponseReader): Value1 {
                val __typename = reader.readString(RESPONSE_FIELDS[0])
                val asStringListSettingValue1 = reader.readConditional(RESPONSE_FIELDS[1]) { conditionalType, reader ->
                    AsStringListSettingValue1(reader)
                }

                return Value1(
                    __typename = __typename,
                    asStringListSettingValue1 = asStringListSettingValue1
                )
            }
        }
    }
}
