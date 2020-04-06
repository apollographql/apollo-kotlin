package com.apollographql.apollo.api.internal

import com.apollographql.apollo.api.ResponseField

interface FieldValueResolver<R> {
  fun <T> valueFor(recordSet: R, field: ResponseField): T?
}
