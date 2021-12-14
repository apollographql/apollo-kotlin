package com.apollographql.apollo3.compiler.introspection

import com.apollographql.apollo3.compiler.fromJson
import com.squareup.moshi.JsonClass
import com.squareup.moshi.JsonReader
import dev.zacsweers.moshix.sealed.annotations.TypeLabel
import okio.Buffer
import okio.BufferedSource
import okio.ByteString.Companion.decodeHex
import okio.buffer
import okio.source
import java.io.File

@JsonClass(generateAdapter = true)
data class IntrospectionSchema(
    val __schema: Schema,
) {
  @JsonClass(generateAdapter = true)
  data class Schema(
      val queryType: QueryType,
      val mutationType: MutationType?,
      val subscriptionType: SubscriptionType?,
      val types: List<Type>,
  ) {
    @JsonClass(generateAdapter = true)
    data class QueryType(val name: String)

    @JsonClass(generateAdapter = true)
    data class MutationType(val name: String)

    @JsonClass(generateAdapter = true)
    data class SubscriptionType(val name: String)

    @JsonClass(generateAdapter = true, generator = "sealed:kind")
    sealed class Type {
      abstract val name: String
      abstract val description: String?

      @TypeLabel("SCALAR")
      @JsonClass(generateAdapter = true)
      data class Scalar(
          override val name: String,
          override val description: String?,
      ) : Type()

      @TypeLabel("OBJECT")
      @JsonClass(generateAdapter = true)
      data class Object(
          override val name: String,
          override val description: String?,
          val fields: List<Field>?,
      ) : Type()

      @TypeLabel("INTERFACE")
      @JsonClass(generateAdapter = true)
      data class Interface(
          override val name: String,
          override val description: String?,
          val fields: List<Field>?,
          val possibleTypes: List<TypeRef>?,
      ) : Type()

      @TypeLabel("UNION")
      @JsonClass(generateAdapter = true)
      data class Union(
          override val name: String,
          override val description: String?,
          val fields: List<Field>?,
          val possibleTypes: List<TypeRef>?,
      ) : Type()

      @TypeLabel("ENUM")
      @JsonClass(generateAdapter = true)
      data class Enum(
          override val name: String,
          override val description: String?,
          val enumValues: List<Value>,
      ) : Type() {


        @JsonClass(generateAdapter = true)
        data class Value(
            val name: String,
            val description: String?,
            val isDeprecated: Boolean = false,
            val deprecationReason: String?,
        )
      }

      @TypeLabel("INPUT_OBJECT")
      @JsonClass(generateAdapter = true)
      data class InputObject(
          override val name: String,
          override val description: String?,
          val inputFields: List<InputField>,
      ) : Type()
    }


    @JsonClass(generateAdapter = true)
    data class InputField(
        val name: String,
        val description: String?,
        val isDeprecated: Boolean = false,
        val deprecationReason: String?,
        val type: TypeRef,
        val defaultValue: Any?,
    )

    @JsonClass(generateAdapter = true)
    data class Field(
        val name: String,
        val description: String?,
        val isDeprecated: Boolean = false,
        val deprecationReason: String?,
        val type: TypeRef,
        val args: List<Argument> = emptyList(),
    ) {

      @JsonClass(generateAdapter = true)
      data class Argument(
          val name: String,
          val description: String?,
          val isDeprecated: Boolean = false,
          val deprecationReason: String?,
          val type: TypeRef,
          val defaultValue: Any?,
      )
    }

    /**
     * An introspection TypeRef
     */
    @JsonClass(generateAdapter = true)
    data class TypeRef(
        val kind: Kind,
        val name: String? = "",
        val ofType: TypeRef? = null,
    )

    enum class Kind {
      ENUM, INTERFACE, OBJECT, INPUT_OBJECT, SCALAR, NON_NULL, LIST, UNION
    }
  }
}

/**
 *
 */
fun BufferedSource.toIntrospectionSchema(origin: String = ""): IntrospectionSchema {
  val bom = "EFBBBF".decodeHex()

  if (rangeEquals(0, bom)) {
    skip(bom.size.toLong())
  }

  return JsonReader.of(this).use {
    try {
      IntrospectionSchema(__schema = it.locateSchemaRootNode().fromJson())
    } catch (e: Exception) {
      throw RuntimeException("Cannot decode introspection $origin", e)
    }
  }
}

fun File.toIntrospectionSchema() = inputStream().source().buffer().toIntrospectionSchema("from `$this`")

fun String.toIntrospectionSchema() = Buffer().writeUtf8(this).toIntrospectionSchema()

private fun JsonReader.locateSchemaRootNode(): JsonReader {
  beginObject()

  var schemaJsonReader: JsonReader? = null
  try {
    while (schemaJsonReader == null && hasNext()) {
      when (nextName()) {
        "data" -> beginObject()
        "__schema" -> schemaJsonReader = peekJson()
        else -> skipValue()
      }
    }
  } catch (e: Exception) {
    throw IllegalArgumentException("Failed to locate schema root node `__schema`", e)
  }

  return schemaJsonReader ?: throw IllegalArgumentException("Failed to locate schema root node `__schema`")
}

fun IntrospectionSchema.normalize(): IntrospectionSchema {
  return copy(
      __schema = __schema.normalize()
  )
}

fun IntrospectionSchema.Schema.normalize(): IntrospectionSchema.Schema {
  return copy(
      types = types.sortedBy { it.name }
  )
}

