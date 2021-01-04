package com.apollographql.apollo.api.internal

import com.apollographql.apollo.api.Operation
import com.apollographql.apollo.api.CustomScalarAdapters
import com.apollographql.apollo.api.internal.json.JsonWriter
import com.apollographql.apollo.api.internal.json.use
import okio.Buffer
import okio.ByteString
import kotlin.jvm.JvmStatic

object OperationRequestBodyComposer {

  @JvmStatic
  fun compose(
      operation: Operation<*, *>,
      autoPersistQueries: Boolean,
      withQueryDocument: Boolean,
      customScalarAdapters: CustomScalarAdapters
  ): ByteString {
    val buffer = Buffer()
    JsonWriter.of(buffer).use { writer ->
      with(writer) {
        serializeNulls = true
        beginObject()
        name("operationName").value(operation.name().name())
        name("variables").jsonValue(operation.variables().marshal(customScalarAdapters))
        if (autoPersistQueries) {
          name("extensions")
          beginObject()
          name("persistedQuery")
          beginObject()
          name("version").value(1)
          name("sha256Hash").value(operation.operationId())
          endObject()
          endObject()
        }
        if (!autoPersistQueries || withQueryDocument) {
          name("query").value(operation.queryDocument())
        }
        endObject()
      }
    }
    return buffer.readByteString()
  }
}
