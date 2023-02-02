@file:Suppress("DEPRECATION_ERROR")
package com.apollographql.apollo3.compiler.introspection

import com.apollographql.apollo3.annotations.ApolloDeprecatedSince
import com.apollographql.apollo3.compiler.fromJson
import com.squareup.moshi.JsonReader
import dev.zacsweers.moshix.sealed.annotations.TypeLabel
import okio.Buffer
import okio.BufferedSource
import okio.ByteString.Companion.decodeHex
import okio.buffer
import okio.source
import java.io.File

@ApolloDeprecatedSince(ApolloDeprecatedSince.Version.v3_3_1)
@Deprecated("Use the apollo-ast version instead", ReplaceWith("IntrospectionSchema", "com.apollographql.apollo3.ast.introspection"), level = DeprecationLevel.ERROR)
data class IntrospectionSchema(
    val __schema: Schema,
) {
  data class Schema(
      val queryType: QueryType,
      val mutationType: MutationType?,
      val subscriptionType: SubscriptionType?,
      val types: List<Type>,
  ) {
    data class QueryType(val name: String)

    data class MutationType(val name: String)

    data class SubscriptionType(val name: String)

    sealed class Type {
      abstract val name: String
      abstract val description: String?

      @TypeLabel("SCALAR")
      data class Scalar(
          override val name: String,
          override val description: String?,
      ) : Type()

      @TypeLabel("OBJECT")
      data class Object(
          override val name: String,
          override val description: String?,
          val fields: List<Field>?,
          val interfaces: List<Interface>?,
      ) : Type()

      @TypeLabel("INTERFACE")
      data class Interface(
          override val name: String,
          override val description: String?,
          val kind: String,
          val fields: List<Field>?,
          val interfaces: List<TypeRef>?,
          val possibleTypes: List<TypeRef>?,
      ) : Type()

      @TypeLabel("UNION")
      data class Union(
          override val name: String,
          override val description: String?,
          val fields: List<Field>?,
          val possibleTypes: List<TypeRef>?,
      ) : Type()

      @TypeLabel("ENUM")
      data class Enum(
          override val name: String,
          override val description: String?,
          val enumValues: List<Value>,
      ) : Type() {


        data class Value(
            val name: String,
            val description: String?,
            val isDeprecated: Boolean = false,
            val deprecationReason: String?,
        )
      }

      @TypeLabel("INPUT_OBJECT")
      data class InputObject(
          override val name: String,
          override val description: String?,
          val inputFields: List<InputField>,
      ) : Type()
    }


    data class InputField(
        val name: String,
        val description: String?,
        val isDeprecated: Boolean = false,
        val deprecationReason: String?,
        val type: TypeRef,
        val defaultValue: Any?,
    )

    data class Field(
        val name: String,
        val description: String?,
        val isDeprecated: Boolean = false,
        val deprecationReason: String?,
        val type: TypeRef,
        val args: List<Argument> = emptyList(),
    ) {

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

