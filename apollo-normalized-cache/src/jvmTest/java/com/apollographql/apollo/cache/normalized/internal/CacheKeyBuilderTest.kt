package com.apollographql.apollo.cache.normalized.internal

import com.apollographql.apollo.api.InputType
import com.apollographql.apollo.api.Operation
import com.apollographql.apollo.api.ResponseField
import com.apollographql.apollo.api.ResponseField.Companion.forString
import com.apollographql.apollo.api.CustomScalar
import com.apollographql.apollo.api.internal.InputFieldMarshaller
import com.apollographql.apollo.api.internal.InputFieldWriter
import com.google.common.truth.Truth
import org.junit.Test
import java.io.IOException
import java.math.BigDecimal
import java.util.HashMap

class CacheKeyBuilderTest {
  private val cacheKeyBuilder: CacheKeyBuilder = RealCacheKeyBuilder()

  internal enum class Episode {
    JEDI
  }

  @Test
  fun testFieldWithNoArguments() {
    val field = forString("hero", "hero", null, false, emptyList())
    val variables: Operation.Variables = object : Operation.Variables() {
      override fun valueMap(): Map<String, Any?> {
        return super.valueMap()
      }
    }
    Truth.assertThat(cacheKeyBuilder.build(field, variables)).isEqualTo("hero")
  }

  @Test
  fun testFieldWithNoArgumentsWithAlias() {
    val field = forString("r2", "hero", null, false, emptyList())
    val variables: Operation.Variables = object : Operation.Variables() {
      override fun valueMap(): Map<String, Any?> {
        return super.valueMap()
      }
    }
    Truth.assertThat(cacheKeyBuilder.build(field, variables)).isEqualTo("hero")
  }

  @Test
  fun testFieldWithArgument() {
    val arguments = mapOf<String, Any?>("episode" to "JEDI")
    val field = createResponseField("hero", "hero", arguments)
    val variables: Operation.Variables = object : Operation.Variables() {
      override fun valueMap(): Map<String, Any?> {
        return super.valueMap()
      }
    }
    Truth.assertThat(cacheKeyBuilder.build(field, variables)).isEqualTo("hero({\"episode\":\"JEDI\"})")
  }

  @Test
  fun testFieldWithArgumentAndAlias() {
    val arguments = mapOf<String, Any?>("episode" to "JEDI")
    val field = createResponseField("r2", "hero", arguments)
    val variables: Operation.Variables = object : Operation.Variables() {
      override fun valueMap(): Map<String, Any?> {
        return super.valueMap()
      }
    }
    Truth.assertThat(cacheKeyBuilder.build(field, variables)).isEqualTo("hero({\"episode\":\"JEDI\"})")
  }

  @Test
  fun testFieldWithVariableArgument() {
    val argument = mapOf<String, Any?>(
        "episode" to mapOf<String, Any>(
            "kind" to "Variable",
            "variableName" to "episode"
        )
    )
    val field = createResponseField("hero", "hero", argument)
    val variables: Operation.Variables = object : Operation.Variables() {
      override fun valueMap(): Map<String, Any?> {
        val map = HashMap<String, Any?>()
        map["episode"] = Episode.JEDI
        return map
      }
    }
    Truth.assertThat(cacheKeyBuilder.build(field, variables)).isEqualTo("hero({\"episode\":\"JEDI\"})")
  }

  @Test
  fun testFieldWithVariableArgumentNull() {
    val argument = mapOf<String, Any?>(
        "episode" to mapOf<String, Any>(
            "kind" to "Variable",
            "variableName" to "episode"
        )
    )
    val field = createResponseField("hero", "hero", argument)
    val variables: Operation.Variables = object : Operation.Variables() {
      override fun valueMap(): Map<String, Any?> {
        val map = HashMap<String, Any?>()
        map["episode"] = null
        return map
      }
    }
    Truth.assertThat(cacheKeyBuilder.build(field, variables)).isEqualTo("hero({\"episode\":null})")
  }

  @Test
  fun testFieldWithMultipleArgument() {
    val arguments = mapOf<String, Any?>(
        "episode" to "JEDI",
        "color" to "blue"
    )
    val field = createResponseField("hero", "hero", arguments)
    val variables: Operation.Variables = object : Operation.Variables() {
      override fun valueMap(): Map<String, Any?> {
        return super.valueMap()
      }
    }
    Truth.assertThat(cacheKeyBuilder.build(field, variables)).isEqualTo("hero({\"color\":\"blue\",\"episode\":\"JEDI\"})")
  }

  @Test
  fun testFieldWithMultipleArgumentsOrderIndependent() {
    val arguments = mapOf<String, Any?>(
        "episode" to "JEDI",
        "color" to "blue"
    )
    val field = createResponseField("hero", "hero", arguments)
    val variables: Operation.Variables = object : Operation.Variables() {
      override fun valueMap(): Map<String, Any?> {
        return super.valueMap()
      }
    }
    val fieldTwoArguments = mapOf<String, Any?>(
        "color" to "blue",
        "episode" to "JEDI")
    val fieldTwo = createResponseField("hero", "hero", fieldTwoArguments)
    Truth.assertThat(cacheKeyBuilder.build(fieldTwo, variables)).isEqualTo(cacheKeyBuilder.build(field, variables))
  }

  @Test
  fun testFieldWithNestedObject() {
    val arguments = mapOf<String, Any?>(
        "episode" to "JEDI",
        "nested" to mapOf<String, Any>(
            "foo" to 1,
            "bar" to 2
        )
    )
    val field = createResponseField("hero", "hero", arguments)
    val variables: Operation.Variables = object : Operation.Variables() {
      override fun valueMap(): Map<String, Any?> {
        return super.valueMap()
      }
    }
    Truth.assertThat(cacheKeyBuilder.build(field, variables)).isEqualTo("hero({\"episode\":\"JEDI\",\"nested\":{\"bar\":2,\"foo\":1}})")
  }

  @Test
  fun testFieldWithNonPrimitiveValue() {
    val field = forString("hero", "hero",
        mapOf<String, Any?>("episode" to Episode.JEDI), false, emptyList())
    val variables: Operation.Variables = object : Operation.Variables() {
      override fun valueMap(): Map<String, Any?> {
        return super.valueMap()
      }
    }
    Truth.assertThat(cacheKeyBuilder.build(field, variables)).isEqualTo("hero({\"episode\":\"JEDI\"})")
  }

  @Test
  fun testFieldWithNestedObjectAndVariables() {
    val arguments = mapOf<String, Any?>(
        "episode" to "JEDI",
        "nested" to mapOf(
            "foo" to mapOf<String, Any>(
                "kind" to "Variable",
                "variableName" to "stars"
            ),
            "bar" to "2"
        )
    )
    val field = createResponseField("hero", "hero", arguments)
    val variables: Operation.Variables = object : Operation.Variables() {
      override fun valueMap(): Map<String, Any?> {
        val map = HashMap<String, Any?>()
        map["stars"] = 1
        return map
      }
    }
    Truth.assertThat(cacheKeyBuilder.build(field, variables)).isEqualTo("hero({\"episode\":\"JEDI\",\"nested\":{\"bar\":\"2\",\"foo\":1}})")
  }

  @Test
  fun fieldInputTypeArgument() {
    val arguments = mapOf<String, Any?>(
        "episode" to "JEDI",
        "nested" to mapOf<String, Any>(
            "foo" to mapOf<String, Any>(
                "kind" to "Variable",
                "variableName" to "testInput"
            ),
            "bar" to "2"
        )
    )
    val field = createResponseField("hero", "hero", arguments)
    val testInput: InputType = object : InputType {
      override fun marshaller(): InputFieldMarshaller {
        return object : InputFieldMarshaller {
          @Throws(IOException::class)
          override fun marshal(writer: InputFieldWriter) {
            writer.writeString("string", "string")
            writer.writeInt("int", 1)
            writer.writeLong("long", 2L)
            writer.writeDouble("double", 3.0)
            writer.writeNumber("number", BigDecimal.valueOf(4))
            writer.writeBoolean("boolean", true)
            writer.writeCustom("custom", object : CustomScalar {
              override val graphqlName = "EPISODE"
              override val className: String
                get() = String::class.java.name
            }, "JEDI")
            writer.writeObject("object", object : InputFieldMarshaller {
              @Throws(IOException::class)
              override fun marshal(writer: InputFieldWriter) {
                writer.writeString("string", "string")
                writer.writeInt("int", 1)
              }
            })
            writer.writeList("list") { listItemWriter ->
              listItemWriter.writeString("string")
              listItemWriter.writeInt(1)
              listItemWriter.writeLong(2L)
              listItemWriter.writeDouble(3.0)
              listItemWriter.writeNumber(BigDecimal.valueOf(4))
              listItemWriter.writeBoolean(true)
              listItemWriter.writeCustom(object : CustomScalar {
                override val graphqlName = "EPISODE"
                override val className: String
                  get() = String::class.java.name
              }, "JEDI")
              listItemWriter.writeObject(object : InputFieldMarshaller {
                @Throws(IOException::class)
                override fun marshal(writer: InputFieldWriter) {
                  writer.writeString("string", "string")
                  writer.writeInt("int", 1)
                }
              })
              listItemWriter.writeList(object : InputFieldWriter.ListWriter {
                @Throws(IOException::class)
                override fun write(listItemWriter: InputFieldWriter.ListItemWriter) {
                  listItemWriter.writeString("string")
                  listItemWriter.writeInt(1)
                }
              })
            }
          }
        }
      }
    }
    val variables: Operation.Variables = object : Operation.Variables() {
      override fun valueMap(): Map<String, Any?> {
        val map = HashMap<String, Any?>()
        map["testInput"] = testInput
        return map
      }
    }
    Truth.assertThat(cacheKeyBuilder.build(field, variables)).isEqualTo(
        "hero({\"episode\":\"JEDI\",\"nested\":{\"bar\":\"2\",\"foo\":{\"boolean\":true,\"custom\":\"JEDI\",\"double\":3.0,\"int\":1,"
            + "\"list\":[\"string\",1,2,3.0,4,true,\"JEDI\",{\"int\":1,\"string\":\"string\"},[\"string\",1]],\"long\":2,"
            + "\"number\":4,\"object\":{\"int\":1,\"string\":\"string\"},\"string\":\"string\"}}})")
  }

  @Test
  fun testFieldArgumentInputTypeWithNulls() {
    val arguments = mapOf<String, Any?>(
        "episode" to null,
        "nested" to mapOf<String, Any?>(
            "foo" to mapOf<String, Any>(
                "kind" to "Variable",
                "variableName" to "testInput"
            ),
            "bar" to null
        )
    )
    val field = createResponseField("hero", "hero", arguments)
    val testInput: InputType = object : InputType {
      override fun marshaller(): InputFieldMarshaller {
        return object : InputFieldMarshaller {
          @Throws(IOException::class)
          override fun marshal(writer: InputFieldWriter) {
            writer.writeString("string", null)
            writer.writeInt("int", null)
            writer.writeLong("long", null)
            writer.writeDouble("double", null)
            writer.writeNumber("number", null)
            writer.writeBoolean("boolean", null)
            writer.writeCustom("custom", object : CustomScalar {
              override val graphqlName = "EPISODE"
              override val className: String
                get() = String::class.java.name
            }, null)
            writer.writeObject("object", null)
            writer.writeList("listNull", null)
            writer.writeList("listWithNulls", object : InputFieldWriter.ListWriter {
              @Throws(IOException::class)
              override fun write(listItemWriter: InputFieldWriter.ListItemWriter) {
                listItemWriter.writeString(null)
                listItemWriter.writeInt(null)
                listItemWriter.writeLong(null)
                listItemWriter.writeDouble(null)
                listItemWriter.writeNumber(null)
                listItemWriter.writeBoolean(null)
                listItemWriter.writeCustom(object : CustomScalar {
                  override val graphqlName = "EPISODE"
                  override val className: String
                    get() = String::class.java.name
                }, null)
                listItemWriter.writeObject(null)
                listItemWriter.writeList(null)
              }
            })
            writer.writeString("null", null)
          }
        }
      }
    }
    val variables: Operation.Variables = object : Operation.Variables() {
      override fun valueMap(): Map<String, Any?> {
        val map = HashMap<String, Any?>()
        map["testInput"] = testInput
        return map
      }
    }
    Truth.assertThat(cacheKeyBuilder.build(field, variables)).isEqualTo("hero({\"episode\":null,\"nested\":{\"bar\":null,\"foo\":{\"boolean\":null,\"custom\":null,\"double\":null,\"int\":null,\"listNull"
        + "\":null,\"listWithNulls\":[],\"long\":null,\"null\":null,\"number\":null,\"object\":null,\"string\":null}}})")
  }

  private fun createResponseField(responseName: String, fieldName: String, arguments: Map<String, Any?>): ResponseField {
    return forString(
        responseName,
        fieldName,
        arguments,
        false, emptyList())
  }

  @Test
  fun testFieldWithVariablesInLists() {
    val arguments = mutableMapOf<String, Any?>().apply {
      put("where", mutableMapOf<String, Any?>().apply {
        put("and", mutableListOf<Any?>().apply {
          add(mutableMapOf<String, Any?>().apply {
            put("kind", "Variable")
            put("variableName", "stars")
          })
        })
      })
    }

    val field = createResponseField("hero", "hero", arguments)
    val variables0: Operation.Variables = object : Operation.Variables() {
      override fun valueMap() = mapOf("stars" to listOf(0))
    }
    val variables1: Operation.Variables = object : Operation.Variables() {
      override fun valueMap() = mapOf("stars" to listOf(1))
    }

    Truth.assertThat(cacheKeyBuilder.build(field, variables0)).isNotEqualTo(cacheKeyBuilder.build(field, variables1))
  }
}
