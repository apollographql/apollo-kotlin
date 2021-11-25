package  com.apollographql.apollo3.api

import com.apollographql.apollo3.api.Executable.Variables
import com.apollographql.apollo3.api.json.BufferedSinkJsonWriter
import com.apollographql.apollo3.api.json.MapJsonWriter
import com.apollographql.apollo3.api.json.JsonWriter
import okio.Buffer
import okio.IOException

/**
 * Base interface for [Operation] and [Fragment] that have an [Adapter] and [Variables].
 *
 * Fragments cannot be executed against a server but can be executed against the cache.
 */
interface Executable<D: Executable.Data> {
  /**
   * The [Adapter] that maps the server response data to/from generated model class [D].
   *
   * This is the low-level API generated by the compiler. Use [parseJsonResponse] and [composeJsonResponse] extension functions for a higher level API
   */
  fun adapter(): Adapter<D>

  /**
   * Serializes the variables of this operation to a json
   */
  @Throws(IOException::class)
  fun serializeVariables(writer: JsonWriter, customScalarAdapters: CustomScalarAdapters)

  /**
   * A list of [CompiledSelection]. Used when reading from the cache and/or normalizing a model.
   * Use [com.apollographql.apollo3.cache.normalized.Store.readOperation] for a higher level API
   */
  fun selections(): List<CompiledSelection>

  /**
   * Marker interface for generated models
   */
  interface Data

  /**
   * A helper class to hold variables
   */
  class Variables(val valueMap: Map<String, Any?>)
}
