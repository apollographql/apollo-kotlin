package com.apollographql.apollo.api.internal

import com.apollographql.apollo.api.Operation
import com.apollographql.apollo.api.ScalarTypeAdapters
import com.apollographql.apollo.api.internal.json.JsonWriter
import okio.Buffer
import okio.ByteString
import kotlin.jvm.JvmStatic

object OperationRequestBodyComposer {

  @JvmStatic
  fun compose(
      operation: Operation<*, *, *>,
      autoPersistQueries: Boolean,
      withQueryDocument: Boolean,
      scalarTypeAdapters: ScalarTypeAdapters
  ): ByteString {
    return Buffer().apply {
      JsonWriter.of(this)
          .apply { serializeNulls = true }
          .beginObject()
          .name("operationName").value(operation.name().name())
          .name("variables").jsonValue(operation.variables().marshal(scalarTypeAdapters))
          .apply {
            if (autoPersistQueries) {
              name("extensions")
                  .beginObject()
                  .name("persistedQuery")
                  .beginObject()
                  .name("version").value(1)
                  .name("sha256Hash").value(operation.operationId())
                  .endObject()
                  .endObject()
            }
          }
          .apply {
            if (withQueryDocument) {
              name("query").value(operation.queryDocument())
            }
          }
          .endObject()
          .close()
    }.readByteString()
  }
}
