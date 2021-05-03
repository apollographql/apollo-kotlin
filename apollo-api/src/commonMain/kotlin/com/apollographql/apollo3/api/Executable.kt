package  com.apollographql.apollo3.api

import com.apollographql.apollo3.api.internal.json.BufferedSinkJsonWriter
import com.apollographql.apollo3.api.internal.json.MapJsonWriter
import com.apollographql.apollo3.api.json.JsonWriter
import okio.Buffer

interface Executable<D: Executable.Data> {
  fun serializeVariables(writer: JsonWriter, responseAdapterCache: CustomScalarAdpaters)

  fun adapter(): Adapter<D>

  fun responseFields(): List<MergedField.FieldSet>

  /**
   * Marker interface for generated models of this fragment
   */
  interface Data

  /**
   * A helper class to hold variables
   */
  class Variables(val valueMap: Map<String, Any?>)
}

/**
 * Serializes variables to a Json Map
 */
@Suppress("UNCHECKED_CAST")
fun <D : Operation.Data> Operation<D>.variables(responseAdapterCache: CustomScalarAdpaters): Executable.Variables {
  val valueMap = MapJsonWriter().apply {
    beginObject()
    serializeVariables(this, responseAdapterCache)
    endObject()
  }.root() as Map<String, Any?>
  return Executable.Variables(valueMap)
}

/**
 * Serializes variables to a Json Map
 */
@Suppress("UNCHECKED_CAST")
fun <D : Fragment.Data> Fragment<D>.variables(responseAdapterCache: CustomScalarAdpaters): Executable.Variables {
  val valueMap = MapJsonWriter().apply {
    beginObject()
    serializeVariables(this, responseAdapterCache)
    endObject()
  }.root() as Map<String, Any?>
  return Executable.Variables(valueMap)
}

/**
 * Serializes variables to a Json String
 */
fun <D : Operation.Data> Operation<D>.variablesJson(responseAdapterCache: CustomScalarAdpaters): String {
  val buffer = Buffer()
  BufferedSinkJsonWriter(buffer).apply {
    beginObject()
    serializeVariables(this, responseAdapterCache)
    endObject()
  }
  return buffer.readUtf8()
}

/**
 * Serializes variables to a Json String
 */
fun <D : Fragment.Data> Fragment<D>.variablesJson(responseAdapterCache: CustomScalarAdpaters): String {
  val buffer = Buffer()
  BufferedSinkJsonWriter(buffer).apply {
    beginObject()
    serializeVariables(this, responseAdapterCache)
    endObject()
  }
  return buffer.readUtf8()
}
