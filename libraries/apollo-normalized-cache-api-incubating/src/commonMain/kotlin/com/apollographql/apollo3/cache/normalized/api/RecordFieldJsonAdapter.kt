package com.apollographql.apollo3.cache.normalized.api

import com.apollographql.apollo3.annotations.ApolloDeprecatedSince
import com.apollographql.apollo3.cache.normalized.api.internal.JsonRecordSerializer

@Deprecated("Use JsonRecordSerializer instead", level = DeprecationLevel.ERROR)
@ApolloDeprecatedSince(ApolloDeprecatedSince.Version.v3_3_1)
object RecordFieldJsonAdapter {
  @Deprecated("Use JsonRecordSerializer instead", ReplaceWith("JsonRecordSerializer.deserialize(json)"), level = DeprecationLevel.ERROR)
  @ApolloDeprecatedSince(ApolloDeprecatedSince.Version.v3_3_1)
  fun fromJson(jsonFieldSource: String): Map<String, Any?> {
    return JsonRecordSerializer.deserialize("", jsonFieldSource).fields
  }

  @Deprecated("Use JsonRecordSerializer instead", ReplaceWith("JsonRecordSerializer.serialize(fields)"), level = DeprecationLevel.ERROR)
  @ApolloDeprecatedSince(ApolloDeprecatedSince.Version.v3_3_1)
  fun toJson(fields: Map<String, Any?>): String {
    return JsonRecordSerializer.serialize(Record("", fields))
  }
}