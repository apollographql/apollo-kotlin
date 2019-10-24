package com.apollographql.apollo.gradle.internal

import com.apollographql.apollo.gradle.api.Introspection


class DefaultIntrospection : Introspection {
  override var endpointUrl: String? = null
  override var queryParameters: Map<String, String>? = null
  override var headers: Map<String, String>? = null
}