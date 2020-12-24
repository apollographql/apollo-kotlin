package com.apollographql.apollo.subscription

import com.apollographql.apollo.api.Operation
import com.apollographql.apollo.api.OperationName
import com.apollographql.apollo.api.Response
import com.apollographql.apollo.api.CustomScalarAdapters
import com.apollographql.apollo.api.Subscription
import com.apollographql.apollo.api.internal.InputFieldMarshaller
import com.apollographql.apollo.api.internal.ResponseFieldMapper
import okio.BufferedSource
import okio.ByteString
import java.io.IOException

class MockSubscription(
    private val queryDocument: String = "subscription{commentAdded{id  name}",
    private val variables: Map<String, Any?> = emptyMap(),
    private val name: String = "SomeSubscription",
    private val operationId: String = "someId"
) : Subscription<Operation.Data, Operation.Variables> {
  override fun queryDocument(): String = queryDocument

  override fun variables(): Operation.Variables = object : Operation.Variables() {
    override fun valueMap(): Map<String, Any?> = variables

    override fun marshaller(): InputFieldMarshaller =
        InputFieldMarshaller { writer ->
          for ((name, value) in variables.entries) {
            when (value) {
              is Number -> writer.writeNumber(name, value)
              is Boolean -> writer.writeBoolean(name, value)
              else -> writer.writeString(name, value.toString())
            }
          }
        }
  }

  override fun responseFieldMapper(): ResponseFieldMapper<Operation.Data> = throw UnsupportedOperationException()

  override fun name(): OperationName =
      object : OperationName {
        override fun name(): String = name
      }

  override fun operationId(): String = operationId

  @kotlin.jvm.Throws(IOException::class)
  override fun parse(source: BufferedSource): Response<Operation.Data> = throw UnsupportedOperationException()

  override fun parse(source: BufferedSource, customScalarAdapters: CustomScalarAdapters): Response<Operation.Data> =
      throw UnsupportedOperationException()

  override fun parse(byteString: ByteString): Response<Operation.Data> =
      throw UnsupportedOperationException()

  override fun parse(byteString: ByteString, customScalarAdapters: CustomScalarAdapters): Response<Operation.Data> =
      throw UnsupportedOperationException()

  override fun composeRequestBody(
      autoPersistQueries: Boolean,
      withQueryDocument: Boolean,
      customScalarAdapters: CustomScalarAdapters): ByteString = throw UnsupportedOperationException()

  override fun composeRequestBody(customScalarAdapters: CustomScalarAdapters): ByteString = throw UnsupportedOperationException()

  override fun composeRequestBody(): ByteString = throw UnsupportedOperationException()
}