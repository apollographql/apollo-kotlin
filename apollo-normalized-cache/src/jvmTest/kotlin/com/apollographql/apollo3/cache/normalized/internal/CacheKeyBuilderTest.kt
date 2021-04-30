package com.apollographql.apollo3.cache.normalized.internal

import com.apollographql.apollo3.api.Executable
import com.apollographql.apollo3.api.ResponseField
import com.apollographql.apollo3.api.Variable
import com.google.common.truth.Truth
import org.junit.Ignore
import org.junit.Test

@Ignore("Will be fixed in the next PR")
class CacheKeyBuilderTest {
  private val cacheKeyBuilder: CacheKeyBuilder = RealCacheKeyBuilder()

  internal enum class Episode {
    JEDI
  }

  @Test
  fun testFieldWithNoArguments() {
    val field = createResponseField("hero", "hero")
    val variables = Executable.Variables(emptyMap())

    Truth.assertThat(cacheKeyBuilder.build(field, variables)).isEqualTo("hero")
  }

  @Test
  fun testFieldWithNoArgumentsWithAlias() {
    val field = createResponseField("r2", "hero")
    val variables = Executable.Variables(emptyMap())

    Truth.assertThat(cacheKeyBuilder.build(field, variables)).isEqualTo("hero")
  }

  @Test
  fun testFieldWithArgument() {
    val arguments = mapOf<String, Any?>("episode" to "JEDI")
    val field = createResponseField("hero", "hero", arguments)
    val variables = Executable.Variables(emptyMap())

    Truth.assertThat(cacheKeyBuilder.build(field, variables)).isEqualTo("hero({\"episode\":\"JEDI\"})")
  }

  @Test
  fun testFieldWithArgumentAndAlias() {
    val arguments = mapOf<String, Any?>("episode" to "JEDI")
    val field = createResponseField("r2", "hero", arguments)
    val variables = Executable.Variables(emptyMap())

    Truth.assertThat(cacheKeyBuilder.build(field, variables)).isEqualTo("hero({\"episode\":\"JEDI\"})")
  }

  @Test
  fun testFieldWithVariableArgument() {
    val argument = mapOf<String, Any?>(
        "episode" to Variable("episode")
    )
    val field = createResponseField("hero", "hero", argument)
    val variables = Executable.Variables(mapOf(
        "episode" to Episode.JEDI
    ))

    Truth.assertThat(cacheKeyBuilder.build(field, variables)).isEqualTo("hero({\"episode\":\"JEDI\"})")
  }

  @Test
  fun testFieldWithVariableArgumentNull() {
    val argument = mapOf<String, Any?>(
        "episode" to Variable("episode")
    )
    val field = createResponseField("hero", "hero", argument)
    val variables = Executable.Variables(mapOf(
        "episode" to null
    ))

    Truth.assertThat(cacheKeyBuilder.build(field, variables)).isEqualTo("hero({\"episode\":null})")
  }

  @Test
  fun testFieldWithMultipleArgument() {
    val arguments = mapOf<String, Any?>(
        "episode" to "JEDI",
        "color" to "blue"
    )
    val field = createResponseField("hero", "hero", arguments)
    val variables = Executable.Variables(emptyMap())

    Truth.assertThat(cacheKeyBuilder.build(field, variables)).isEqualTo("hero({\"color\":\"blue\",\"episode\":\"JEDI\"})")
  }

  @Test
  fun testFieldWithMultipleArgumentsOrderIndependent() {
    val arguments = mapOf<String, Any?>(
        "episode" to "JEDI",
        "color" to "blue"
    )
    val field = createResponseField("hero", "hero", arguments)
    val variables = Executable.Variables(emptyMap())

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
    val variables = Executable.Variables(emptyMap())

    Truth.assertThat(cacheKeyBuilder.build(field, variables)).isEqualTo("hero({\"episode\":\"JEDI\",\"nested\":{\"bar\":2,\"foo\":1}})")
  }

  @Test
  fun testFieldWithNonPrimitiveValue() {
    val field = ResponseField(
        type = ResponseField.Type.Named.Other("String"),
        fieldName = "hero",
        arguments = mapOf<String, Any?>("episode" to Episode.JEDI)
    )

    val variables = Executable.Variables(emptyMap())
    Truth.assertThat(cacheKeyBuilder.build(field, variables)).isEqualTo("hero({\"episode\":\"JEDI\"})")
  }

  @Test
  fun testFieldWithNestedObjectAndVariables() {
    val arguments = mapOf<String, Any?>(
        "episode" to "JEDI",
        "nested" to mapOf(
            "foo" to Variable("stars"),
            "bar" to "2"
        )
    )
    val field = createResponseField("hero", "hero", arguments)
    val variables = Executable.Variables(mapOf("stars" to 1))

    Truth.assertThat(cacheKeyBuilder.build(field, variables)).isEqualTo("hero({\"episode\":\"JEDI\",\"nested\":{\"bar\":\"2\",\"foo\":1}})")
  }

  @Test
  fun fieldInputTypeArgument() {
    val arguments = mapOf<String, Any?>(
        "episode" to "JEDI",
        "nested" to mapOf(
            "foo" to Variable("testInput"),
            "bar" to "2"
        )
    )
    val field = createResponseField("hero", "hero", arguments)
    val testInput = mapOf(
        "string" to "string",
        "int" to 1,
        "double" to 3.0,
        "boolean" to true,
        "custom" to "JEDI",
        "object" to mapOf(
            "string" to "string",
            "int" to 1,
        ),
        "list" to listOf(
            "string",
            1,
            3.0,
            true,
            "JEDI",
            mapOf(
                "string" to "string",
                "int" to 1,
            ),
            listOf(
                "string",
                1
            )
        )
    )

    val variables = Executable.Variables(mapOf("testInput" to testInput))

    Truth.assertThat(cacheKeyBuilder.build(field, variables)).isEqualTo(
        "hero({\"episode\":\"JEDI\",\"nested\":{\"bar\":\"2\",\"foo\":{\"string\":\"string\",\"int\":1,\"double\":3.0,\"boolean\":true,\"custom\":\"JEDI\",\"object\":{\"string\":\"string\",\"int\":1},\"list\":[\"string\",1,3.0,true,\"JEDI\",{\"string\":\"string\",\"int\":1},[\"string\",1]]}}})")
  }

  @Test
  fun testFieldArgumentInputTypeWithNulls() {
    val arguments = mapOf(
        "episode" to null,
        "nested" to mapOf(
            "foo" to Variable("testInput"),
            "bar" to null
        )
    )
    val field = createResponseField("hero", "hero", arguments)
    val testInput = mapOf(
        "string" to null,
        "int" to null,
        "long" to null,
        "double" to null,
        "number" to null,
        "boolean" to null,
        "custom" to null,
        "object" to null,
        "listNull" to null,
        "listWithNulls" to listOf(null, null, null, null, null, null, null, null, null),
        "null" to null
    )

    val variables = Executable.Variables(mapOf("testInput" to testInput))

    Truth.assertThat(cacheKeyBuilder.build(field, variables)).isEqualTo("hero({\"episode\":null,\"nested\":{\"bar\":null,\"foo\":{\"string\":null,\"int\":null,\"long\":null,\"double\":null,\"number\":null,\"boolean\":null,\"custom\":null,\"object\":null,\"listNull\":null,\"listWithNulls\":[null,null,null,null,null,null,null,null,null],\"null\":null}}})")
  }

  private fun createResponseField(responseName: String, fieldName: String, arguments: Map<String, Any?> = emptyMap()): ResponseField {
    return ResponseField(
        type = ResponseField.Type.Named.Other("String"),
        fieldName = fieldName,
        responseName = responseName,
        arguments = arguments
    )
  }

  @Test
  fun testFieldWithVariablesInLists() {
    val arguments = mapOf(
        "where" to mapOf(
            "and" to listOf(
                Variable("stars")
            )
        )
    )

    val field = createResponseField("hero", "hero", arguments)
    val variables0 = Executable.Variables(mapOf("stars" to listOf(0)))
    val variables1 = Executable.Variables(mapOf("stars" to listOf(1)))

    Truth.assertThat(cacheKeyBuilder.build(field, variables0)).isNotEqualTo(cacheKeyBuilder.build(field, variables1))
  }
}
