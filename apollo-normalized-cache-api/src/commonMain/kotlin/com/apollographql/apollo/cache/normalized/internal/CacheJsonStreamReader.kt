package com.apollographql.apollo.cache.normalized.internal

import com.apollographql.apollo.api.internal.Throws
import com.apollographql.apollo.api.internal.json.JsonReader
import com.apollographql.apollo.api.internal.json.ResponseJsonStreamReader
import com.apollographql.apollo.cache.normalized.CacheReference
import okio.IOException

/**
 * A [ResponseJsonStreamReader] with additional support for [CacheReference].
 */
class CacheJsonStreamReader(jsonReader: JsonReader) : ResponseJsonStreamReader(jsonReader) {

  @Throws(IOException::class)
  override fun nextScalar(optional: Boolean): Any? {
    val scalar = super.nextScalar(optional)
    return if (scalar is String && CacheReference.canDeserialize(scalar)) {
      CacheReference.deserialize(scalar)
    } else {
      scalar
    }
  }
}
