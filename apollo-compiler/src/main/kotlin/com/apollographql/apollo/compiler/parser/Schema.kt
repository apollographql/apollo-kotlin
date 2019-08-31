package com.apollographql.apollo.compiler.parser

import com.squareup.moshi.JsonClass
import com.squareup.moshi.JsonReader
import com.squareup.moshi.Moshi
import com.squareup.moshi.adapters.PolymorphicJsonAdapterFactory
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okio.Okio
import java.io.File

@JsonClass(generateAdapter = true)
class Schema(
    val queryType: String = "query",
    val mutationType: String = "mutation",
    val subscriptionType: String = "subscription",
    val types: Map<String, Type>) : Map<String, Schema.Type> by types {
  sealed class Type(val kind: Kind) {
    abstract val name: String
    abstract val description: String?

    @JsonClass(generateAdapter = true)
    data class Scalar(
        override val name: String,
        override val description: String?
    ) : Type(Kind.SCALAR)

    @JsonClass(generateAdapter = true)
    data class Object(
        override val name: String,
        override val description: String?,
        val fields: List<Field>?
    ) : Type(Kind.OBJECT)

    @JsonClass(generateAdapter = true)
    data class Interface(
        override val name: String,
        override val description: String?,
        val fields: List<Field>?,
        val possibleTypes: List<TypeRef>?
    ) : Type(Kind.INTERFACE)

    @JsonClass(generateAdapter = true)
    data class Union(
        override val name: String,
        override val description: String?,
        val fields: List<Field>?,
        val possibleTypes: List<TypeRef>?
    ) : Type(Kind.UNION)

    @JsonClass(generateAdapter = true)
    data class Enum(
        override val name: String,
        override val description: String?,
        val enumValues: List<Value>
    ) : Type(Kind.ENUM) {

      @JsonClass(generateAdapter = true)
      data class Value(
          val name: String,
          val description: String?,
          val isDeprecated: Boolean = false,
          val deprecationReason: String?
      )
    }

    @JsonClass(generateAdapter = true)
    data class InputObject(
        override val name: String,
        override val description: String?,
        val inputFields: List<InputField>
    ) : Type(Kind.INPUT_OBJECT)
  }

  @JsonClass(generateAdapter = true)
  data class InputField(
      val name: String,
      val description: String?,
      val isDeprecated: Boolean = false,
      val deprecationReason: String?,
      val type: TypeRef,
      val defaultValue: Any?
  )

  @JsonClass(generateAdapter = true)
  data class Field(
      val name: String,
      val description: String?,
      val isDeprecated: Boolean = false,
      val deprecationReason: String?,
      val type: TypeRef,
      val args: List<Argument> = emptyList()
  ) {

    @JsonClass(generateAdapter = true)
    data class Argument(
        val name: String,
        val description: String?,
        val isDeprecated: Boolean = false,
        val deprecationReason: String?,
        val type: TypeRef,
        val defaultValue: Any?
    )
  }

  @JsonClass(generateAdapter = true)
  data class TypeRef(
      val kind: Kind,
      val name: String? = "",
      val ofType: TypeRef? = null
  ) {
    val rawType: TypeRef = ofType?.rawType ?: this
  }

  enum class Kind {
    ENUM, INTERFACE, OBJECT, INPUT_OBJECT, SCALAR, NON_NULL, LIST, UNION
  }

  companion object {
    @JvmStatic
    @JvmName("parse")
    operator fun invoke(schemaFile: File): Schema {
      val moshi = Moshi.Builder()
          .add(
              PolymorphicJsonAdapterFactory.of(Type::class.java, "kind")
                  .withSubtype(Type.Scalar::class.java, Kind.SCALAR.name)
                  .withSubtype(Type.Object::class.java, Kind.OBJECT.name)
                  .withSubtype(Type.Interface::class.java, Kind.INTERFACE.name)
                  .withSubtype(Type.Union::class.java, Kind.UNION.name)
                  .withSubtype(Type.Enum::class.java, Kind.ENUM.name)
                  .withSubtype(Type.InputObject::class.java, Kind.INPUT_OBJECT.name)
          )
          .add(KotlinJsonAdapterFactory())
          .build()

      val source = try {
        Okio.buffer(Okio.source(schemaFile.inputStream()))
      } catch (e: Exception) {
        throw RuntimeException("Failed to parse GraphQL schema introspection query from `$schemaFile`", e)
      }

      val jsonReader = JsonReader.of(source)
      return try {
        jsonReader.locateSchemaRootNode().parseSchema(moshi)
      } catch (e: Exception) {
        throw RuntimeException("Failed to parse GraphQL schema introspection query from `$schemaFile`", e)
      } finally {
        jsonReader.close()
      }
    }

    private fun JsonReader.locateSchemaRootNode(): JsonReader {
      beginObject()
      return when (nextName()) {
        "data" -> {
          beginObject()
          if (nextName() == "__schema") {
            peekJson()
          } else {
            throw IllegalArgumentException("Failed to locate schema root node `__schema`")
          }
        }

        "__schema" -> peekJson()

        else -> throw IllegalArgumentException("Failed to locate schema root node `__schema`")
      }
    }

    private fun JsonReader.parseSchema(moshi: Moshi): Schema {
      val introspectionSchema = moshi.adapter(IntrospectionQuery.Schema::class.java).fromJson(this)!!
      return Schema(
          queryType = introspectionSchema.queryType?.name ?: "query",
          mutationType = introspectionSchema.mutationType?.name ?: "mutation",
          subscriptionType = introspectionSchema.subscriptionType?.name ?: "subscription",
          types = introspectionSchema.types.associateBy { it.name }
      )
    }
  }
}

object IntrospectionQuery {
  @JsonClass(generateAdapter = true)
  data class QueryType(val name: String)

  @JsonClass(generateAdapter = true)
  data class MutationType(val name: String)

  @JsonClass(generateAdapter = true)
  data class SubscriptionType(val name: String)

  @JsonClass(generateAdapter = true)
  data class Schema(
      val queryType: QueryType?,
      val mutationType: MutationType?,
      val subscriptionType: SubscriptionType?,
      val types: List<com.apollographql.apollo.compiler.parser.Schema.Type>
  )
}
