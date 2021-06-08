package com.apollographql.apollo3

import com.apollographql.apollo3.api.ClientContext
import com.apollographql.apollo3.api.ExecutionContext
import kotlinx.coroutines.CoroutineScope

class ClientScope(val coroutineScope: CoroutineScope): ClientContext(Key) {
  companion object Key: ExecutionContext.Key<ClientScope>
}