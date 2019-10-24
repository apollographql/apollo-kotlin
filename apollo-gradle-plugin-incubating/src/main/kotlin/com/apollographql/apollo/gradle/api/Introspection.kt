package com.apollographql.apollo.gradle.api

interface Introspection {
  var endpointUrl: String?
  var queryParameters: Map<String, String>?
  var headers: Map<String, String>?
}