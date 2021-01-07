package com.apollographql.apollo.api.internal

import com.apollographql.apollo.api.ResponseField

interface ValueResolver<R> {
  fun <T> valueFor(recordSet: R, field: ResponseField): T?
}
