package com.apollographql.apollo3.api

import com.apollographql.apollo3.api.json.JsonWriter
import com.apollographql.apollo3.api.json.writeAny
import com.apollographql.apollo3.api.json.writeArray
import com.apollographql.apollo3.api.json.writeObject
import kotlin.jvm.JvmOverloads

/**
 * Write a GraphQL Json response containing "data" (and "error" if any) to the given JsonWriter.
 *
 * Use this for testing/mocking a GraphQL response
 */
@JvmOverloads
fun <D : Operation.Data> ApolloResponse<D>.composeJsonResponse(
    jsonWriter: JsonWriter,
    customScalarAdapters: CustomScalarAdapters = CustomScalarAdapters.Empty,
) {
  jsonWriter.writeObject {
    name("data")
    data?.let {
      operation.adapter().toJson(this, customScalarAdapters, it)
    } ?: nullValue()

    if (hasErrors()) {
      name("errors")
      writeArray {
        for (error in errors!!) {
          writeObject {
            error.path?.let { pathElements ->
              name("path")
              writeArray {
                for (pathElement in pathElements) {
                  if (pathElement is String) value(pathElement) else value(pathElement as Int)
                }
              }
            }

            error.locations?.let { locations ->
              name("locations")
              writeArray {
                for (location in locations) {
                  writeObject {
                    name("line")
                    value(location.line)
                    name("column")
                    value(location.column)
                  }
                }
              }
            }

            error.extensions?.let { extensions ->
              name("extensions")
              writeAny(extensions)
            }

            name("message")
            value(error.message)
          }
        }
      }
    }
  }
}
