package com.apollographql.apollo3

import com.apollographql.apollo3.api.ExecutionContext
import com.apollographql.apollo3.api.ExecutionParameters
import kotlinx.coroutines.CoroutineScope

class ClientScope(val coroutineScope: CoroutineScope): ExecutionContext.Element {
  override val key: ExecutionContext.Key<*>
    get() = Key
  companion object Key: ExecutionContext.Key<ClientScope>
}



