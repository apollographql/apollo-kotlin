package com.apollographql.apollo.api.internal

import com.apollographql.apollo.api.ResponseField

interface ValueResolver<in R> {
  fun <T> valueFor(map: R, field: ResponseField): T?
}
