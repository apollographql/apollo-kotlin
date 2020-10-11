package com.apollographql.apollo

import com.apollographql.apollo.api.cache.http.HttpCacheRecord
import com.apollographql.apollo.api.cache.http.HttpCacheRecordEditor
import com.apollographql.apollo.api.cache.http.HttpCacheStore
import java.io.IOException

internal class MockHttpCacheStore : HttpCacheStore {
  var delegate: HttpCacheStore? = null
  @Throws(IOException::class)
  override fun cacheRecord(cacheKey: String): HttpCacheRecord? {
    return delegate!!.cacheRecord(cacheKey)
  }

  @Throws(IOException::class)
  override fun cacheRecordEditor(cacheKey: String): HttpCacheRecordEditor? {
    return delegate!!.cacheRecordEditor(cacheKey)
  }

  @Throws(IOException::class)
  override fun remove(cacheKey: String) {
    delegate!!.remove(cacheKey)
  }

  @Throws(IOException::class)
  override fun delete() {
    delegate!!.delete()
  }
}