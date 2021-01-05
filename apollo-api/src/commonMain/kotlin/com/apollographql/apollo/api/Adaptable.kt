package com.apollographql.apollo.api

import com.apollographql.apollo.api.internal.ResponseAdapter

interface Adaptable<T> {
  fun adapter(): ResponseAdapter<T>
}