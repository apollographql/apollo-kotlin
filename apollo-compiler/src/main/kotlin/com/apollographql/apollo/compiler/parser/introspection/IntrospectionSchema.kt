package com.apollographql.apollo.compiler.parser.introspection

import com.apollographql.apollo.compiler.fromJson
import com.squareup.moshi.JsonClass
import com.squareup.moshi.JsonReader
import okio.ByteString.Companion.decodeHex
import okio.buffer
import okio.source
import java.io.File
import java.io.InputStream

data class IntrospectionSchema(
    val queryType: String,
    val mutationType: String?,
    val subscriptionType: String?,
    val types: Map<String, Type>) : Map<String, IntrospectionSchema.Type> by types {
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

  private fun validate(): IntrospectionSchema {
    return copy(
        types = types.mapValues {
          when (val type = it.value) {
            is Type.Object -> type.validate()
            is Type.Enum -> type.validate()
            is Type.InputObject -> type.validate()
            else -> type
          }
        }
    )
  }

  private fun Type.Object.validate() = copy(fields = fields?.map { it.validate() })

  private fun Type.Enum.validate() = copy(enumValues = enumValues.map { it.validate() })

  private fun Type.InputObject.validate() = copy(inputFields = inputFields.map { it.validate() })

  private fun InputField.validate(): InputField {
    return when {
      isDeprecated && deprecationReason == null -> {
        println("InputField '$name' is marked as deprecated but did not provide a reason. Falling back to 'No longer supported'")
        copy(deprecationReason = "No longuer supported")
      }
      !isDeprecated && deprecationReason !== null -> {
        println("InputField '$name' is marked as not deprecated but provided a reason. Marking as deprecated")
        copy(isDeprecated = true)
      }
      else -> this
    }
  }

  private fun Type.Enum.Value.validate(): Type.Enum.Value {
    return when {
      isDeprecated && deprecationReason == null -> {
        println("EnumValue '$name' is marked as deprecated but did not provide a reason. Falling back to 'No longer supported'")
        copy(deprecationReason = "No longuer supported")
      }
      !isDeprecated && deprecationReason !== null -> {
        println("EnumValue '$name' is marked as not deprecated but provided a reason. Marking as deprecated")
        copy(isDeprecated = true)
      }
      else -> this
    }
  }

  private fun Field.validate(): Field {
    return when {
      isDeprecated && deprecationReason == null -> {
        println("Field '$name' is marked as deprecated but did not provide a reason. Falling back to 'No longer supported'")
        copy(deprecationReason = "No longuer supported")
      }
      !isDeprecated && deprecationReason !== null -> {
        println("Field '$name' is marked as not deprecated but provided a reason. Marking as deprecated")
        copy(isDeprecated = true)
      }
      else -> this
    }.copy(args = args.map {
      it.validate()
    })
  }

  private fun Field.Argument.validate(): Field.Argument {
    return when {
      isDeprecated && deprecationReason == null -> {
        println("Argument '$name' is marked as deprecated but did not provide a reason. Falling back to 'No longer supported'")
        copy(deprecationReason = "No longuer supported")
      }
      !isDeprecated && deprecationReason !== null -> {
        println("Argument '$name' is marked as not deprecated but provided a reason. Marking as deprecated")
        copy(isDeprecated = true)
      }
      else -> this
    }
  }

  companion object {
    private val UTF8_BOM = "EFBBBF".decodeHex()

    operator fun invoke(inputStream: InputStream, origin: String = ""): IntrospectionSchema {
      val source = try {
        inputStream.source().buffer()
      } catch (e: Exception) {
        throw RuntimeException("Failed to parse GraphQL schema introspection query $origin", e)
      }

      if (source.rangeEquals(0, UTF8_BOM)) {
        source.skip(UTF8_BOM.size.toLong())
      }

      val jsonReader = JsonReader.of(source)
      return try {
        jsonReader.locateSchemaRootNode().fromJson<IntrospectionQuery.Schema>().toIntrospectionSchema()
      } catch (e: Exception) {
        throw RuntimeException("Failed to parse GraphQL schema introspection query $origin", e)
      } finally {
        jsonReader.close()
      }
    }

    operator fun invoke(schemaFile: File) = IntrospectionSchema(schemaFile.inputStream(), "from `$schemaFile`")

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

    fun IntrospectionQuery.Schema.toIntrospectionSchema(): IntrospectionSchema {
      return IntrospectionSchema(
          queryType = queryType?.name ?: "Query",
          mutationType = mutationType?.name,
          subscriptionType = subscriptionType?.name,
          types = types.associateBy { it.name }
      ).validate()
    }

    fun IntrospectionSchema.wrap(): IntrospectionQuery.Wrapper {
      return IntrospectionQuery.Wrapper(
          __schema = IntrospectionQuery.Schema(
              queryType = this.queryType.let { IntrospectionQuery.QueryType(it) },
              mutationType = this.mutationType?.let { IntrospectionQuery.MutationType(it) },
              subscriptionType = this.subscriptionType?.let { IntrospectionQuery.SubscriptionType(it) },
              types = types.values.toList()
          )
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

  /**
   * An intermediate class that matches the introspection query results
   */
  @JsonClass(generateAdapter = true)
  data class Schema(
      val queryType: QueryType?,
      val mutationType: MutationType?,
      val subscriptionType: SubscriptionType?,
      val types: List<IntrospectionSchema.Type>
  )

  @JsonClass(generateAdapter = true)
  data class Wrapper(
      val __schema: Schema
  )
}
