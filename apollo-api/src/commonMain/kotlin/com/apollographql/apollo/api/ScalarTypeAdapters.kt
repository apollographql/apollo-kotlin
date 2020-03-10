package com.apollographql.apollo.api

expect class ScalarTypeAdapters(customAdapters: Map<ScalarType, CustomTypeAdapter<*>>) {

  fun <T : Any> adapterFor(scalarType: ScalarType): CustomTypeAdapter<T>

  companion object {
    val DEFAULT: ScalarTypeAdapters
  }
}
