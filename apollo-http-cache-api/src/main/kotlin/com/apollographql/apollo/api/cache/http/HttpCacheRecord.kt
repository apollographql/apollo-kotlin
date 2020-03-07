package com.apollographql.apollo.api.cache.http

import okio.Source

interface HttpCacheRecord {
  fun headerSource(): Source
  fun bodySource(): Source
  fun close()
}
