package com.apollographql.apollo3

import com.apollographql.apollo3.api.ExecutionContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope

class ConcurrencyInfo(
    val dispatcher: CoroutineDispatcher,
    val coroutineScope: CoroutineScope,
) : ExecutionContext.Element {
  override val key: ExecutionContext.Key<*>
    get() = Key

  companion object Key : ExecutionContext.Key<ConcurrencyInfo>
}



