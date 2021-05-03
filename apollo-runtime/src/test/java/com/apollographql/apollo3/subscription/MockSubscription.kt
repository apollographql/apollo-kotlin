package com.apollographql.apollo3.subscription

import com.apollographql.apollo3.api.CustomScalarAdpaters
import com.apollographql.apollo3.api.MergedField
import com.apollographql.apollo3.api.Subscription
import com.apollographql.apollo3.api.AnyAdapter
import com.apollographql.apollo3.api.json.JsonWriter
import com.apollographql.apollo3.api.nullable

class MockSubscription(
    private val queryDocument: String = "subscription{commentAdded{id  name}",
    private val variables: Map<String, Any?> = emptyMap(),
    private val name: String = "SomeSubscription",
    private val operationId: String = "someId"
) : Subscription<Subscription.Data> {
  override fun document(): String = queryDocument

  override fun serializeVariables(writer: JsonWriter, responseAdapterCache: CustomScalarAdpaters) {
    variables.forEach {
      writer.name(it.key)
      AnyAdapter.nullable().toJson(writer, responseAdapterCache, it.value)
    }
  }

  override fun adapter() = throw UnsupportedOperationException()

  override fun name(): String = name

  override fun id(): String = operationId
  override fun responseFields(): List<MergedField.FieldSet> {
    return emptyList()
  }
}
