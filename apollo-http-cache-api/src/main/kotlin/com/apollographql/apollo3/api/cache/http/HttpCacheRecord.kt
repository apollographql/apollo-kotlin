package com.apollographql.apollo3.api.cache.http

import okio.Source

interface HttpCacheRecord {
  fun headerSource(): Source
  fun bodySource(): Source
  fun close()
}
