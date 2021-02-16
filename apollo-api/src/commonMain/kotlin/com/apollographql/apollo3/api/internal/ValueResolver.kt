package com.apollographql.apollo3.api.internal

import com.apollographql.apollo3.api.ResponseField

interface ValueResolver<in R> {
  fun <T> valueFor(map: R, field: ResponseField): T?
}
