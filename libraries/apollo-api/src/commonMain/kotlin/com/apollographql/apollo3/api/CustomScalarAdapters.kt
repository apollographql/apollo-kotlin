package com.apollographql.apollo3.api

import com.apollographql.apollo3.annotations.ApolloDeprecatedSince
import com.apollographql.apollo3.annotations.ApolloDeprecatedSince.Version.v3_0_0
import com.apollographql.apollo3.annotations.ApolloDeprecatedSince.Version.v3_2_1
import com.apollographql.apollo3.annotations.ApolloExperimental
import com.apollographql.apollo3.api.internal.Version2CustomTypeAdapterToAdapter
import kotlin.jvm.JvmField

/**
 * A wrapper around a Map<String, [Adapter]> used to retrieve custom scalar adapters at runtime
 */
class CustomScalarAdapters private constructor(
    customScalarAdapters: Map<String, Adapter<*>>,
    // We piggyback CustomScalarAdapters to pass around a context which is used in the Adapters at parse time.
    // This is currently used for @skip/@include and @defer.
    // Ideally it should be passed as its own parameter, but we're avoiding a breaking change.
    // See https://github.com/apollographql/apollo-kotlin/pull/3813
    /**
     * Note: this shouldn't be part of the public API and will be removed in Apollo Kotlin 4. If you needed this, please open an issue.
     */
    val adapterContext: AdapterContext,
    private val unsafe: Boolean,
) : ExecutionContext.Element {

  private val adaptersMap: Map<String, Adapter<*>> = customScalarAdapters

  fun <T : Any> responseAdapterFor(customScalar: CustomScalarType): Adapter<T> {
    @Suppress("UNCHECKED_CAST")
    return when {
      adaptersMap[customScalar.name] != null -> {
        adaptersMap[customScalar.name]
      }
      /**
       * Below are shortcuts to save the users a call to `registerCustomScalarAdapter`
       */
      customScalar.className == "com.apollographql.apollo3.api.Upload" -> {
        UploadAdapter
      }
      customScalar.className in listOf("kotlin.String", "java.lang.String") -> {
        StringAdapter
      }
      customScalar.className in listOf("kotlin.Boolean", "java.lang.Boolean") -> {
        BooleanAdapter
      }
      customScalar.className in listOf("kotlin.Int", "java.lang.Int") -> {
        IntAdapter
      }
      customScalar.className in listOf("kotlin.Double", "java.lang.Double") -> {
        DoubleAdapter
      }
      customScalar.className in listOf("kotlin.Long", "java.lang.Long") -> {
        LongAdapter
      }
      customScalar.className in listOf("kotlin.Float", "java.lang.Float") -> {
        FloatAdapter
      }
      customScalar.className in listOf("kotlin.Any", "java.lang.Object") -> {
        AnyAdapter
      }
      unsafe -> PassThroughAdapter()
      else -> error("Can't map GraphQL type: `${customScalar.name}` to: `${customScalar.className}`. Did you forget to add a CustomScalarAdapter?")
    } as Adapter<T>
  }

  @Deprecated("Use adapterContext.variables() instead", ReplaceWith("adapterContext.variables()"))
  @ApolloDeprecatedSince(v3_2_1)
  @Suppress("DEPRECATION")
  fun variables() = adapterContext.variables()

  override val key: ExecutionContext.Key<*>
    get() = Key

  companion object Key : ExecutionContext.Key<CustomScalarAdapters> {
    /**
     * An empty [CustomScalarAdapters]. If the models were generated with some custom scalars, parsing will fail
     */
    @JvmField
    val Empty = Builder().build()

    /**
     * Unsafe [CustomScalarAdapters]. They can only be used with `MapJsonReader` and `MapJsonWriter`. It will passthrough the values using
     * `MapJsonReader.nextValue` and `MapJsonWriter.value()`
     */
    @JvmField
    @ApolloExperimental
    val PassThrough = Builder().unsafe(true).build()
  }

  fun newBuilder() = Builder().addAll(this)

  class Builder {
    private val adaptersMap: MutableMap<String, Adapter<*>> = mutableMapOf()
    private var adapterContext: AdapterContext = AdapterContext.Builder().build()
    private var unsafe = false

    fun <T> add(
        customScalarType: CustomScalarType,
        customScalarAdapter: Adapter<T>,
    ) = apply {
      adaptersMap[customScalarType.name] = customScalarAdapter
    }

    @Suppress("DEPRECATION")
    @Deprecated("Used for backward compatibility with 2.x")
    @ApolloDeprecatedSince(v3_0_0)
    fun <T> add(
        customScalarType: CustomScalarType,
        customTypeAdapter: CustomTypeAdapter<T>,
    ) = apply {
      adaptersMap[customScalarType.name] = Version2CustomTypeAdapterToAdapter(customTypeAdapter)
    }

    fun addAll(customScalarAdapters: CustomScalarAdapters) = apply {
      this.adaptersMap.putAll(customScalarAdapters.adaptersMap)
    }

    @ApolloExperimental
    fun unsafe(unsafe: Boolean) = apply {
      this.unsafe = unsafe
    }

    fun clear() {
      adaptersMap.clear()
    }

    fun build() = CustomScalarAdapters(adaptersMap, adapterContext, unsafe)

    fun adapterContext(adapterContext: AdapterContext): Builder = apply {
      this.adapterContext = adapterContext
    }

    @Deprecated("Use AdapterContext.Builder.variables() instead")
    @ApolloDeprecatedSince(v3_2_1)
    fun variables(variables: Executable.Variables): Builder = apply {
      adapterContext = adapterContext.newBuilder().variables(variables).build()
    }
  }
}
