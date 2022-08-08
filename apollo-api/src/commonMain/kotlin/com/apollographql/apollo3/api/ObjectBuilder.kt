package com.apollographql.apollo3.api

import com.apollographql.apollo3.annotations.ApolloInternal
import com.apollographql.apollo3.api.json.MapJsonReader

@Suppress("PropertyName")
abstract class ObjectBuilder {
  protected val __fields = mutableMapOf<String, Any?>()

  var __typename: String by __fields

  operator fun set(key: String, value: Any) {
    __fields[key] = value
  }
}

