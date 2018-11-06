package com.example.nested_inline_fragment

import com.apollographql.apollo.api.Operation
import com.apollographql.apollo.api.OperationName
import com.apollographql.apollo.api.Query
import com.apollographql.apollo.api.ResponseField
import com.apollographql.apollo.api.ResponseFieldMapper
import com.apollographql.apollo.api.ResponseFieldMarshaller
import com.apollographql.apollo.api.ResponseReader
import com.example.nested_inline_fragment.fragment.TestSetting
import javax.annotation.Generated
import kotlin.Array
import kotlin.String

@Generated("Apollo GraphQL")
class TestQuery : Query<TestQuery.Data, TestQuery.Data, Operation.Variables> {
    override fun operationId(): String = OPERATION_ID
    override fun queryDocument(): String = QUERY_DOCUMENT
    override fun wrapData(data: TestQuery.Data): TestQuery.Data = data
    override fun variables(): Operation.Variables = Operation.EMPTY_VARIABLES
    override fun name(): OperationName = OPERATION_NAME
    override fun responseFieldMapper(): ResponseFieldMapper<TestQuery.Data> = ResponseFieldMapper {
        TestQuery.Data(it)
    }

    data class Setting(val __typename: String, val fragments: Fragments) {
        fun marshaller(): ResponseFieldMarshaller = ResponseFieldMarshaller {
            it.writeString(RESPONSE_FIELDS[0], __typename)
            fragments.marshaller().marshal(it)
        }

        companion object {
            private val RESPONSE_FIELDS: Array<ResponseField> = arrayOf(
                    ResponseField.forString("__typename", "__typename", null, false, null),
                    ResponseField.forString("__typename", "__typename", null, false, null)
                    )

            operator fun invoke(reader: ResponseReader): Setting {
                val __typename = reader.readString(RESPONSE_FIELDS[0])
                val fragments = reader.readConditional(RESPONSE_FIELDS[1]) { conditionalType, reader ->
                    val testSetting = if (TestSetting.POSSIBLE_TYPES.contains(conditionalType)) TestSetting(reader) else null
                    Fragments(
                        testSetting = testSetting!!
                    )
                }

                return Setting(
                    __typename = __typename,
                    fragments = fragments
                )
            }
        }

        data class Fragments(val testSetting: TestSetting) {
            fun marshaller(): ResponseFieldMarshaller = ResponseFieldMarshaller {
                testSetting.marshaller().marshal(it)
            }
        }
    }

    data class Data(val setting: Setting) : Operation.Data {
        override fun marshaller(): ResponseFieldMarshaller = ResponseFieldMarshaller {
            it.writeObject(RESPONSE_FIELDS[0], setting.marshaller())
        }

        companion object {
            private val RESPONSE_FIELDS: Array<ResponseField> = arrayOf(
                    ResponseField.forObject("setting", "setting", null, false, null)
                    )

            operator fun invoke(reader: ResponseReader): Data {
                val setting = reader.readObject<Setting>(RESPONSE_FIELDS[0]) {
                    Setting(it)
                }

                return Data(
                    setting = setting
                )
            }
        }
    }

    companion object {
        val OPERATION_DEFINITION: String = """
                |query TestOperation {
                |  setting {
                |    __typename
                |    ...TestSetting
                |  }
                |}
                """.trimMargin()

        const val OPERATION_ID: String =
                "c4e875b8b3292e1ca6a36e8dccd11c724ee40eed3a3b87a1107fceddb3186fd2"

        val QUERY_DOCUMENT: String = """
                |query TestOperation {
                |  setting {
                |    __typename
                |    ...TestSetting
                |  }
                |}
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

        val OPERATION_NAME: OperationName = OperationName { "TestQuery" }
    }
}
