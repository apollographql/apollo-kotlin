package com.apollographql.apollo3.execution

import com.apollographql.apollo3.api.Error
import com.apollographql.apollo3.api.json.JsonWriter
import com.apollographql.apollo3.api.json.writeAny
import com.apollographql.apollo3.api.json.writeArray
import com.apollographql.apollo3.api.json.writeObject
import okio.Sink

/**
 * @property errors if non-null, errors must contain at least 1 error
 */
class GraphQLResponse internal constructor(val data: Any?, val errors: List<Error>?, val extensions: Map<String, Any?>?) {
  class Builder {
    var data: Map<String, Any?>? = null
    var errors: List<Error>? = null
    var extensions: Map<String, Any?>? = null

    fun data(data: Map<String, Any?>?): Builder = apply {
      this.data = data
    }

    fun errors(errors: List<Error>?): Builder = apply {
      this.errors = errors
    }

    fun extensions(extensions: Map<String, Any?>?): Builder = apply {
      this.extensions = extensions
    }

    fun build(): GraphQLResponse {
      return GraphQLResponse(
          data,
          errors,
          extensions
      )
    }
  }

  fun serialize(jsonWriter: JsonWriter) {
      jsonWriter.writeObject {
          name("data")
          writeAny(data)
          if (!errors.isNullOrEmpty()) {
              name("errors")
              writeArray {
                  errors.forEach {
                      writeError(it)
                  }
              }
          }
          if (extensions != null) {
              name("extensions")
              writeAny(extensions)
          }
      }
      jsonWriter.flush()
  }

  fun serialize(sink: Sink) {
    serialize(sink.jsonWriter())
  }
}

internal fun JsonWriter.writeError(error: Error) {
    writeObject {
        name("message")
        value(error.message)
        if (error.locations != null) {
            name("locations")
            writeArray {
                error.locations!!.forEach {
                    writeObject {
                        name("line")
                        value(it.line)
                        name("column")
                        value(it.column)
                    }
                }
            }
        }
        if (error.path != null) {
            name("path")
            writeArray {
                error.path!!.forEach {
                    when (it) {
                        is Int -> value(it)
                        is String -> value(it)
                        else -> error("path can only contain Int and Double (found '${it::class.simpleName}')")
                    }
                }
            }
        }
        if (error.extensions != null) {
            name("extensions")
            writeObject {
                error.extensions!!.entries.forEach {
                    name(it.key)
                    writeAny(it.value)
                }
            }
        }
    }
}