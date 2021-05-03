package com.apollographql.apollo3.subscription

import com.apollographql.apollo3.api.CustomScalarAdapters
import com.apollographql.apollo3.api.Subscription
import com.apollographql.apollo3.api.AnyAdapter
import com.apollographql.apollo3.api.FieldSet
import com.apollographql.apollo3.api.json.JsonWriter
import com.apollographql.apollo3.api.nullable

class MockSubscription(
    private val queryDocument: String = "subscription{commentAdded{id  name}",
    private val variables: Map<String, Any?> = emptyMap(),
    private val name: String = "SomeSubscription",
    private val operationId: String = "someId"
) : Subscription<Subscription.Data> {
  override fun document(): String = queryDocument

  override fun serializeVariables(writer: JsonWriter, customScalarAdapters: CustomScalarAdapters) {
    variables.forEach {
      writer.name(it.key)
      AnyAdapter.nullable().toJson(writer, customScalarAdapters, it.value)
    }
  }

  override fun adapter() = throw UnsupportedOperationException()

  override fun name(): String = name

  override fun id(): String = operationId
  override fun fieldSets(): List<FieldSet> {
    return emptyList()
  }
}
