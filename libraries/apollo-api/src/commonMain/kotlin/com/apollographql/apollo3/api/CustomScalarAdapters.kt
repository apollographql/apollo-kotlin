@file:Suppress("DEPRECATION")

package com.apollographql.apollo3.api

import com.apollographql.apollo3.annotations.ApolloDeprecatedSince
import com.apollographql.apollo3.annotations.ApolloDeprecatedSince.Version.v4_0_0
import com.apollographql.apollo3.annotations.ApolloInternal
import kotlin.jvm.JvmField

@Deprecated("Use ScalarAdapters instead")
@ApolloDeprecatedSince(v4_0_0)
class CustomScalarAdapters private constructor(
    customScalarAdapters: Map<String, Adapter<*>>,
) : ExecutionContext.Element {

  private val adaptersMap: Map<String, Adapter<*>> = customScalarAdapters

  fun <T : Any> responseAdapterFor(customScalar: ScalarType): Adapter<T> {
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

      else -> error("Can't map GraphQL type: `${customScalar.name}` to: `${customScalar.className}`. Did you forget to add a CustomScalarAdapter?")
    } as Adapter<T>
  }

  @ApolloInternal
  fun toScalarAdapters(): ScalarAdapters {
    return ScalarAdapters.Builder()
        .apply {
          for ((name, adapter) in adaptersMap) {
            add(ScalarType(name, ""), AdapterToScalarAdapter(adapter))
          }
        }
        .build()
  }

  override val key: ExecutionContext.Key<*>
    get() = Key

  companion object Key : ExecutionContext.Key<CustomScalarAdapters> {
    /**
     * An empty [CustomScalarAdapters]. If the models were generated with some custom scalars, parsing will fail
     */
    @JvmField
    val Empty = Builder().build()
  }

  fun newBuilder() = Builder().addAll(this)

  class Builder {
    private val adaptersMap: MutableMap<String, Adapter<*>> = mutableMapOf()

    fun <T> add(
        customScalarType: ScalarType,
        customScalarAdapter: Adapter<T>,
    ) = apply {
      adaptersMap[customScalarType.name] = customScalarAdapter
    }

    fun addAll(customScalarAdapters: CustomScalarAdapters) = apply {
      this.adaptersMap.putAll(customScalarAdapters.adaptersMap)
    }

    fun clear() {
      adaptersMap.clear()
    }

    fun build() = CustomScalarAdapters(adaptersMap)
  }
}
