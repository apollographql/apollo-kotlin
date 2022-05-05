package com.apollographql.apollo3.cache.normalized.api

import com.apollographql.apollo3.annotations.ApolloInternal
import com.apollographql.apollo3.cache.normalized.api.internal.JsonRecordSerializer

@OptIn(ApolloInternal::class)
@Deprecated("Use JsonRecordSerializer instead")
object RecordFieldJsonAdapter {
  @Deprecated("Use JsonRecordSerializer instead", ReplaceWith("JsonRecordSerializer.deserialize(json)"))
  fun fromJson(jsonFieldSource: String): Map<String, Any?> {
    return JsonRecordSerializer.deserialize("", jsonFieldSource).fields
  }

  @Deprecated("Use JsonRecordSerializer instead", ReplaceWith("JsonRecordSerializer.serialize(fields)"))
  fun toJson(fields: Map<String, Any?>): String {
    return JsonRecordSerializer.serialize(Record("", fields))
  }
}