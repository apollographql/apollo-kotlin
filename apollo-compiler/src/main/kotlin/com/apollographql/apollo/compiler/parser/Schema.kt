package com.apollographql.apollo.compiler.parser

import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.adapters.PolymorphicJsonAdapterFactory
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okio.Okio
import java.io.File
import java.lang.RuntimeException

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
    operator fun invoke(schemaFile: File): Schema {
      val moshi = Moshi.Builder()
          .add(
              PolymorphicJsonAdapterFactory.of(Schema.Type::class.java, "kind")
                  .withSubtype(Schema.Type.Scalar::class.java, Schema.Kind.SCALAR.name)
                  .withSubtype(Schema.Type.Object::class.java, Schema.Kind.OBJECT.name)
                  .withSubtype(Schema.Type.Interface::class.java, Schema.Kind.INTERFACE.name)
                  .withSubtype(Schema.Type.Union::class.java, Schema.Kind.UNION.name)
                  .withSubtype(Schema.Type.Enum::class.java, Schema.Kind.ENUM.name)
                  .withSubtype(Schema.Type.InputObject::class.java, Schema.Kind.INPUT_OBJECT.name)
          )
          .add(KotlinJsonAdapterFactory())
          .build()
      try {
        val source = Okio.buffer(Okio.source(schemaFile.inputStream()))
        val introspection = moshi.adapter(IntrospectionQuery::class.java).fromJson(source)
        return Schema(
            queryType = introspection!!.data.__schema.queryType?.name ?: "query",
            mutationType = introspection.data.__schema.mutationType?.name ?: "mutation",
            subscriptionType = introspection.data.__schema.subscriptionType?.name ?: "subscription",
            types = introspection.data.__schema.types.associateBy { it.name }
        )
      } catch (e: Exception) {
        throw RuntimeException("Failed to parse GraphQL schema introspection query from `$schemaFile`", e)
      }
    }
  }
}

@JsonClass(generateAdapter = true)
data class IntrospectionQuery(val data: Data) {
  @JsonClass(generateAdapter = true)
  data class Data(val __schema: Schema)

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
