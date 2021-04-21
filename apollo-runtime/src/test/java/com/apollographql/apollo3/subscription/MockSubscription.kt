package com.apollographql.apollo3.subscription

import com.apollographql.apollo3.api.ResponseAdapterCache
import com.apollographql.apollo3.api.Operation
import com.apollographql.apollo3.api.ResponseField
import com.apollographql.apollo3.api.Subscription
import com.apollographql.apollo3.api.AnyResponseAdapter
import com.apollographql.apollo3.api.json.JsonWriter
import com.apollographql.apollo3.api.nullable

class MockSubscription(
    private val queryDocument: String = "subscription{commentAdded{id  name}",
    private val variables: Map<String, Any?> = emptyMap(),
    private val name: String = "SomeSubscription",
    private val operationId: String = "someId"
) : Subscription<Subscription.Data> {
  override fun document(): String = queryDocument

  override fun serializeVariables(writer: JsonWriter, responseAdapterCache: ResponseAdapterCache) {
    variables.forEach {
      writer.name(it.key)
      AnyResponseAdapter.nullable().toResponse(writer, responseAdapterCache, it.value)
    }
  }

  override fun adapter() = throw UnsupportedOperationException()

  override fun name(): String = name

  override fun id(): String = operationId
  override fun responseFields(): List<ResponseField.FieldSet> {
    return emptyList()
  }
}
