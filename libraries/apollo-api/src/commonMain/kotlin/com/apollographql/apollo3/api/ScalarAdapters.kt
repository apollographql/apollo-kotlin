package com.apollographql.apollo3.api

import com.apollographql.apollo3.annotations.ApolloExperimental
import com.apollographql.apollo3.annotations.ApolloInternal
import com.apollographql.apollo3.api.json.JsonReader
import com.apollographql.apollo3.api.json.JsonWriter
import kotlin.jvm.JvmField

/**
 * A wrapper around a Map<String, [DataAdapter]> used to retrieve scalar adapters at runtime
 */
class ScalarAdapters private constructor(
    scalarAdapters: Map<String, DataAdapter<*>>,
    private val unsafe: Boolean,
) : ExecutionContext.Element {

  private val adaptersMap: Map<String, DataAdapter<*>> = scalarAdapters

  fun <T : Any> responseAdapterFor(scalar: ScalarType): DataAdapter<T> {
    @Suppress("UNCHECKED_CAST")
    return when {
      adaptersMap[scalar.name] != null -> {
        adaptersMap[scalar.name]
      }
      /**
       * Below are shortcuts to save the users a call to `registerScalarAdapter`
       */
      scalar.className == "com.apollographql.apollo3.api.Upload" -> {
        UploadDataAdapter
      }

      scalar.className in listOf("kotlin.String", "java.lang.String") -> {
        StringDataAdapter
      }

      scalar.className in listOf("kotlin.Boolean", "java.lang.Boolean") -> {
        BooleanDataAdapter
      }

      scalar.className in listOf("kotlin.Int", "java.lang.Int") -> {
        IntDataAdapter
      }

      scalar.className in listOf("kotlin.Double", "java.lang.Double") -> {
        DoubleDataAdapter
      }

      scalar.className in listOf("kotlin.Long", "java.lang.Long") -> {
        LongDataAdapter
      }

      scalar.className in listOf("kotlin.Float", "java.lang.Float") -> {
        FloatDataAdapter
      }

      scalar.className in listOf("kotlin.Any", "java.lang.Object") -> {
        AnyDataAdapter
      }

      unsafe -> PassThroughDataAdapter()
      else -> error("Can't map GraphQL type: `${scalar.name}` to: `${scalar.className}`. Did you forget to add a ScalarAdapter?")
    } as DataAdapter<T>
  }

  @ApolloInternal
  @Suppress("DEPRECATION")
  fun toCustomScalarAdapters(): CustomScalarAdapters {
    return CustomScalarAdapters.Builder()
        .apply {
          for ((name, adapter) in adaptersMap) {
            add(ScalarType(name, ""), DataAdapterToAdapter(adapter))
          }
        }
        .build()
  }

  @Suppress("DEPRECATION")
  private class DataAdapterToAdapter<T>(private val wrappedAdapter: DataAdapter<T>) : Adapter<T> {
    override fun fromJson(reader: JsonReader, customScalarAdapters: CustomScalarAdapters): T {
      return wrappedAdapter.fromJson(reader, Empty)
    }

    override fun toJson(writer: JsonWriter, customScalarAdapters: CustomScalarAdapters, value: T) {
      wrappedAdapter.toJson(writer, Empty, value)
    }
  }


  override val key: ExecutionContext.Key<*>
    get() = Key

  companion object Key : ExecutionContext.Key<ScalarAdapters> {
    /**
     * An empty [ScalarAdapters]. If the models were generated with some custom scalars, parsing will fail
     */
    @JvmField
    val Empty = Builder().build()

    /**
     * Unsafe [ScalarAdapters]. They can only be used with `MapJsonReader` and `MapJsonWriter`. It will passthrough the values using
     * `MapJsonReader.nextValue` and `MapJsonWriter.value()`
     */
    @JvmField
    @ApolloExperimental
    val PassThrough = Builder().unsafe(true).build()
  }

  fun newBuilder() = Builder().addAll(this)

  class Builder {
    private val adaptersMap: MutableMap<String, DataAdapter<*>> = mutableMapOf()
    private var unsafe = false

    fun <T> add(
        scalarType: ScalarType,
        scalarAdapter: ScalarAdapter<T>,
    ) = apply {
      adaptersMap[scalarType.name] = ScalarAdapterToDataAdapter(scalarAdapter)
    }

    fun addAll(scalarAdapters: ScalarAdapters) = apply {
      this.adaptersMap.putAll(scalarAdapters.adaptersMap)
    }

    @ApolloExperimental
    fun unsafe(unsafe: Boolean) = apply {
      this.unsafe = unsafe
    }

    fun clear() {
      adaptersMap.clear()
    }

    fun build() = ScalarAdapters(adaptersMap, unsafe)
  }
}
