package com.apollographql.apollo.api.cache.http

import okio.Sink
import java.io.IOException

interface HttpCacheRecordEditor {
  fun headerSink(): Sink
  fun bodySink(): Sink

  @Throws(IOException::class)
  fun abort()

  @Throws(IOException::class)
  fun commit()
}
