@file:Suppress("DEPRECATION")

package com.apollographql.apollo.testing

import com.apollographql.apollo.annotations.ApolloDeprecatedSince
import com.apollographql.apollo.annotations.ApolloInternal

enum class Platform {
  Jvm,
  Native,
  Js,
  WasmJs
}

expect fun platform(): Platform