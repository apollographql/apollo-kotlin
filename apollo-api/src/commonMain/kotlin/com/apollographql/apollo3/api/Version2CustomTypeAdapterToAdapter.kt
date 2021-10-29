package com.apollographql.apollo3.api

import com.apollographql.apollo3.api.json.JsonReader
import com.apollographql.apollo3.api.json.JsonWriter
import kotlin.jvm.JvmStatic

/**
 * An [Adapter] that wraps an Apollo Android v2 style [CustomTypeAdapter], to ease migration from v2 to v3.
 */
@Deprecated("Used for backward compatibility with 2.x, use Adapter instead")
class Version2CustomTypeAdapterToAdapter<T>(
    private val v2CustomTypeAdapter: CustomTypeAdapter<T>,
) : Adapter<T> {
  override fun fromJson(reader: JsonReader, customScalarAdapters: CustomScalarAdapters): T {
    val value: Any? = if (reader.peek() == JsonReader.Token.NULL) null else AnyAdapter.fromJson(reader)
    return v2CustomTypeAdapter.decode(CustomTypeValue(value))
  }

  override fun toJson(writer: JsonWriter, customScalarAdapters: CustomScalarAdapters, value: T) {
    val encoded: Any? = v2CustomTypeAdapter.encode(value).value
    if (encoded == null) {
      writer.nullValue()
    } else {
      AnyAdapter.toJson(writer, encoded)
    }
  }
}

/**
 * A replica of Apollo Android v2's CustomTypeAdapter, to ease migration from v2 to v3.
 *
 * Make your CustomTypeAdapters implement this interface by updating the imports
 * from `com.apollographql.apollo.api` to `com.apollographql.apollo3.api`
 * and wrap them in [Version2CustomTypeAdapterToAdapter]s, which you can add to
 * `ApolloClient.Builder` or [CustomScalarAdapters].
 */
@Deprecated("Used for backward compatibility with 2.x, use Adapter instead")
interface CustomTypeAdapter<T> {
  fun decode(value: CustomTypeValue<*>): T
  fun encode(value: T): CustomTypeValue<*>
}

/**
 * A replica of Apollo Android v2's CustomTypeValue, to ease migration from v2 to v3.
 *
 * In your [CustomTypeAdapter], update the imports from `com.apollographql.apollo.api` to
 * `com.apollographql.apollo3.api` to use this version.
 */
@Deprecated("Used for backward compatibility with 2.x, use Adapter instead")
open class CustomTypeValue<T>(val value: T) {
  object GraphQLNull : CustomTypeValue<Unit>(Unit)
  class GraphQLString(value: String) : CustomTypeValue<String>(value)
  class GraphQLBoolean(value: Boolean) : CustomTypeValue<Boolean>(value)
  class GraphQLNumber(value: Number) : CustomTypeValue<Number>(value)
  class GraphQLJsonObject(value: Map<String, Any>) : CustomTypeValue<Map<String, Any>>(value)
  class GraphQLJsonList(value: List<Any>) : CustomTypeValue<List<Any>>(value)

  companion object {
    @JvmStatic
    @Suppress("UNCHECKED_CAST")
    fun fromRawValue(value: Any): CustomTypeValue<*> {
      return when (value) {
        is Map<*, *> -> GraphQLJsonObject(value as Map<String, Any>)
        is List<*> -> GraphQLJsonList(value as List<Any>)
        is Boolean -> GraphQLBoolean(value)
        // Not supported as we are in common code here
        /* is BigDecimal -> GraphQLNumber(value.toNumber()) */
        is Number -> GraphQLNumber(value)
        else -> GraphQLString(value.toString())
      }
    }
  }
}
