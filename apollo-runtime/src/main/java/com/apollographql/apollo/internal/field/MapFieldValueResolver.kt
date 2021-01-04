package com.apollographql.apollo.internal.field

import com.apollographql.apollo.api.ResponseField
import com.apollographql.apollo.api.internal.FieldValueResolver

class MapFieldValueResolver : FieldValueResolver<Map<String, Any>> {
  override fun <T> valueFor(map: Map<String, Any>, field: ResponseField): T? {
    return map[field.responseName] as T?
  }
}