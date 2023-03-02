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
private class IntrospectionSchemaEnvelope(
    val data: IntrospectionSchema?,
    val __schema: IntrospectionSchema.Schema?,
)

@Serializable
class IntrospectionSchema(
    val __schema: Schema,
) {
  @Serializable
  class Schema(
      val queryType: QueryType,
      val mutationType: MutationType?,
      val subscriptionType: SubscriptionType?,
      val types: List<Type>,
      val directives: List<Directive> = emptyList(),
  ) {
    @Serializable
    class QueryType(val name: String)

    @Serializable
    class MutationType(val name: String)

    @Serializable
    class SubscriptionType(val name: String)

    @Serializable
    sealed class Type {
      abstract val name: String
      abstract val description: String?

      @SerialName("SCALAR")
      @Serializable
      class Scalar(
          override val name: String,
          override val description: String?,
      ) : Type()

      @SerialName("OBJECT")
      @Serializable
      class Object(
          override val name: String,
          override val description: String?,
          val fields: List<Field>?,
          val interfaces: List<Interface>?,
      ) : Type()

      @SerialName("INTERFACE")
      @Serializable
      class Interface(
          override val name: String,
          override val description: String?,
          val fields: List<Field>?,
          val interfaces: List<TypeRef>?,
          val possibleTypes: List<TypeRef>?,
      ) : Type()

      @SerialName("UNION")
      @Serializable
      class Union(
          override val name: String,
          override val description: String?,
          val fields: List<Field>?,
          val possibleTypes: List<TypeRef>?,
      ) : Type()

      @SerialName("ENUM")
      @Serializable
      class Enum(
          override val name: String,
          override val description: String?,
          val enumValues: List<Value>,
      ) : Type() {
        @Serializable
        class Value(
            val name: String,
            val description: String?,
            val isDeprecated: Boolean = false,
            val deprecationReason: String?,
        )
      }

      @SerialName("INPUT_OBJECT")
      @Serializable
      class InputObject(
          override val name: String,
          override val description: String?,
          val inputFields: List<InputField>,
      ) : Type()
    }

    @Serializable
    class InputField(
        val name: String,
        val description: String?,
        val isDeprecated: Boolean = false,
        val deprecationReason: String?,
        val type: TypeRef,
        val defaultValue: String?,
    )

    @Serializable
    class Field(
        val name: String,
        val description: String?,
        val isDeprecated: Boolean = false,
        val deprecationReason: String?,
        val type: TypeRef,
        val args: List<Argument> = emptyList(),
    )

    @Serializable
    class Argument(
        val name: String,
        val description: String?,
        val isDeprecated: Boolean = false,
        val deprecationReason: String?,
        val type: TypeRef,
        val defaultValue: String?,
    )

    /**
     * An introspection TypeRef
     */
    @Serializable
    class TypeRef(
        val kind: Kind,
        val name: String? = "",
        val ofType: TypeRef? = null,
    )

    /**
     * An introspection directive
     */
    @Serializable
    class Directive(
        val name: String,
        val description: String?,
        val locations: List<DirectiveLocation> = emptyList(),
        val args: List<Argument>,
        val isRepeatable: Boolean = false,
    ) {
      enum class DirectiveLocation {
        QUERY,
        MUTATION,
        SUBSCRIPTION,
        FIELD,
        FRAGMENT_DEFINITION,
        FRAGMENT_SPREAD,
        INLINE_FRAGMENT,
        VARIABLE_DEFINITION,
        SCHEMA,
        SCALAR,
        OBJECT,
        FIELD_DEFINITION,
        ARGUMENT_DEFINITION,
        INTERFACE,
        UNION,
        ENUM,
        ENUM_VALUE,
        INPUT_OBJECT,
        INPUT_FIELD_DEFINITION,
      }
    }

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
    introspectionSchemaEnvelope.data ?: introspectionSchemaEnvelope.__schema?.let { schema ->
      IntrospectionSchema(IntrospectionSchema.Schema(
          queryType = schema.queryType,
          mutationType = schema.mutationType,
          subscriptionType = schema.subscriptionType,
          types = schema.types,
          // Old introspection json (pre `April2016`) may not have the `locations` field, in which case the list will be empty, which is invalid. Exclude those directives.
          // Validation doesn't validate the unknown directives (yet). A future version may want to fail here and enforce proper validation.
          directives = schema.directives.filter { directive -> directive.locations.isNotEmpty() }
      ))
    }
    ?: throw IllegalArgumentException("Invalid introspection schema: $origin")
  } catch (e: Exception) {
    throw RuntimeException("Cannot decode introspection $origin", e)
  }
}

fun File.toIntrospectionSchema() = inputStream().source().buffer().toIntrospectionSchema("from `$this`")

fun String.toIntrospectionSchema() = Buffer().writeUtf8(this).toIntrospectionSchema()

fun IntrospectionSchema.normalize(): IntrospectionSchema {
  return IntrospectionSchema(
      __schema = __schema.normalize()
  )
}

fun IntrospectionSchema.Schema.normalize(): IntrospectionSchema.Schema {
  return IntrospectionSchema.Schema(
      queryType = queryType,
      mutationType = mutationType,
      subscriptionType = subscriptionType,
      types = types.sortedBy { it.name },
      directives = directives.sortedBy { it.name }
  )
}

fun IntrospectionSchema.toJson(): String {
  return json.encodeToString(IntrospectionSchema.serializer(), this)
}

fun IntrospectionSchema.writeTo(file: File) {
  file.outputStream().sink().buffer().use {
    it.writeUtf8(json.encodeToString(IntrospectionSchema.serializer(), this))
  }
}
