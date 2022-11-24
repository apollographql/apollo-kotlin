package com.apollographql.apollo3.ast.introspection

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okio.Buffer
import okio.BufferedSource
import okio.ByteString.Companion.decodeHex
import okio.buffer
import okio.sink
import okio.source
import java.io.File

@Serializable
private data class IntrospectionSchemaEnvelope(
    val data: IntrospectionSchema?,
    val __schema: IntrospectionSchema.Schema?,
)

@Serializable
data class IntrospectionSchema(
    val __schema: Schema,
) {
  @Serializable
  data class Schema(
      val queryType: QueryType,
      val mutationType: MutationType?,
      val subscriptionType: SubscriptionType?,
      val types: List<Type>,
  ) {
    @Serializable
    data class QueryType(val name: String)

    @Serializable
    data class MutationType(val name: String)

    @Serializable
    data class SubscriptionType(val name: String)

    @Serializable
    sealed class Type {
      abstract val name: String
      abstract val description: String?

      @SerialName("SCALAR")
      @Serializable
      data class Scalar(
          override val name: String,
          override val description: String?,
      ) : Type()

      @SerialName("OBJECT")
      @Serializable
      data class Object(
          override val name: String,
          override val description: String?,
          val fields: List<Field>?,
          val interfaces: List<Interface>?,
      ) : Type()

      @SerialName("INTERFACE")
      @Serializable
      data class Interface(
          override val name: String,
          override val description: String?,
          val fields: List<Field>?,
          val interfaces: List<TypeRef>?,
          val possibleTypes: List<TypeRef>?,
      ) : Type()

      @SerialName("UNION")
      @Serializable
      data class Union(
          override val name: String,
          override val description: String?,
          val fields: List<Field>?,
          val possibleTypes: List<TypeRef>?,
      ) : Type()

      @SerialName("ENUM")
      @Serializable
      data class Enum(
          override val name: String,
          override val description: String?,
          val enumValues: List<Value>,
      ) : Type() {
        @Serializable
        data class Value(
            val name: String,
            val description: String?,
            val isDeprecated: Boolean = false,
            val deprecationReason: String?,
        )
      }

      @SerialName("INPUT_OBJECT")
      @Serializable
      data class InputObject(
          override val name: String,
          override val description: String?,
          val inputFields: List<InputField>,
      ) : Type()
    }

    @Serializable
    data class InputField(
        val name: String,
        val description: String?,
        val isDeprecated: Boolean = false,
        val deprecationReason: String?,
        val type: TypeRef,
        val defaultValue: String?,
    )

    @Serializable
    data class Field(
        val name: String,
        val description: String?,
        val isDeprecated: Boolean = false,
        val deprecationReason: String?,
        val type: TypeRef,
        val args: List<Argument> = emptyList(),
    ) {

      @Serializable
      data class Argument(
          val name: String,
          val description: String?,
          val isDeprecated: Boolean = false,
          val deprecationReason: String?,
          val type: TypeRef,
          val defaultValue: String?,
      )
    }

    /**
     * An introspection TypeRef
     */
    @Serializable
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

private val json: Json by lazy {
  Json {
    ignoreUnknownKeys = true
    classDiscriminator = "kind"
    @OptIn(ExperimentalSerializationApi::class)
    explicitNulls = false
  }
}

fun BufferedSource.toIntrospectionSchema(origin: String = ""): IntrospectionSchema {
  val bom = "EFBBBF".decodeHex()

  if (rangeEquals(0, bom)) {
    skip(bom.size.toLong())
  }

  return try {
    val introspectionSchemaEnvelope = json.decodeFromString(IntrospectionSchemaEnvelope.serializer(), this.readUtf8())
    introspectionSchemaEnvelope.data ?: introspectionSchemaEnvelope.__schema?.let { IntrospectionSchema(it) }
    ?: throw IllegalArgumentException("Invalid introspection schema: $origin")
  } catch (e: Exception) {
    throw RuntimeException("Cannot decode introspection $origin", e)
  }
}

fun File.toIntrospectionSchema() = inputStream().source().buffer().toIntrospectionSchema("from `$this`")

fun String.toIntrospectionSchema() = Buffer().writeUtf8(this).toIntrospectionSchema()

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

fun IntrospectionSchema.toJson(): String {
  return json.encodeToString(IntrospectionSchema.serializer(), this)
}

fun IntrospectionSchema.toJson(file: File) {
  file.outputStream().sink().buffer().use {
    it.writeUtf8(json.encodeToString(IntrospectionSchema.serializer(), this))
  }
}
