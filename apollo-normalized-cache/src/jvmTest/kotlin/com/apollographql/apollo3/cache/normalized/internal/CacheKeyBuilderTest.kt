package com.apollographql.apollo3.cache.normalized.internal

import com.apollographql.apollo3.api.CompiledArgument
import com.apollographql.apollo3.api.CompiledField
import com.apollographql.apollo3.api.CompiledStringType
import com.apollographql.apollo3.api.CompiledVariable
import com.apollographql.apollo3.api.Executable
import com.google.common.truth.Truth
import org.junit.Test

class CacheKeyBuilderTest {
  private val cacheKeyBuilder: CacheKeyBuilder = RealCacheKeyBuilder()

  internal enum class Episode {
    JEDI
  }

  @Test
  fun testFieldWithNoArguments() {
    val field = createCompiledField("hero", "hero")
    val variables = Executable.Variables(emptyMap())

    Truth.assertThat(cacheKeyBuilder.build(field, variables)).isEqualTo("hero")
  }

  @Test
  fun testFieldWithNoArgumentsWithAlias() {
    val field = createCompiledField("r2", "hero")
    val variables = Executable.Variables(emptyMap())

    Truth.assertThat(cacheKeyBuilder.build(field, variables)).isEqualTo("hero")
  }

  @Test
  fun testFieldWithArgument() {
    val arguments = listOf(
        CompiledArgument("episode", "JEDI")
    )

    val field = createCompiledField("hero", "hero", arguments)
    val variables = Executable.Variables(emptyMap())

    Truth.assertThat(cacheKeyBuilder.build(field, variables)).isEqualTo("hero({\"episode\":\"JEDI\"})")
  }

  @Test
  fun testFieldWithArgumentAndAlias() {
    val arguments = listOf(
        CompiledArgument("episode", "JEDI")
    )

    val field = createCompiledField("r2", "hero", arguments)
    val variables = Executable.Variables(emptyMap())

    Truth.assertThat(cacheKeyBuilder.build(field, variables)).isEqualTo("hero({\"episode\":\"JEDI\"})")
  }

  @Test
  fun testFieldWithVariableArgument() {
    val argument = listOf(
        CompiledArgument("episode", CompiledVariable("episode"))
    )
    val field = createCompiledField("hero", "hero", argument)
    val variables = Executable.Variables(mapOf(
        "episode" to Episode.JEDI
    ))

    Truth.assertThat(cacheKeyBuilder.build(field, variables)).isEqualTo("hero({\"episode\":\"JEDI\"})")
  }

  @Test
  fun testFieldWithVariableArgumentNull() {
    val argument = listOf(
        CompiledArgument("episode", CompiledVariable("episode"))
    )
    val field = createCompiledField("hero", "hero", argument)
    val variables = Executable.Variables(mapOf(
        "episode" to null
    ))

    Truth.assertThat(cacheKeyBuilder.build(field, variables)).isEqualTo("hero({\"episode\":null})")
  }

  @Test
  fun testFieldWithMultipleArgument() {
    val arguments = listOf(
        CompiledArgument("episode", "JEDI"),
        CompiledArgument("color", "blue")
    )
    val field = createCompiledField("hero", "hero", arguments)
    val variables = Executable.Variables(emptyMap())

    Truth.assertThat(cacheKeyBuilder.build(field, variables)).isEqualTo("hero({\"color\":\"blue\",\"episode\":\"JEDI\"})")
  }

  @Test
  fun testFieldWithMultipleArgumentsOrderIndependent() {
    val arguments = listOf(
        CompiledArgument("episode", "JEDI"),
        CompiledArgument("color", "blue")
    )
    val field = createCompiledField("hero", "hero", arguments)
    val variables = Executable.Variables(emptyMap())

    val fieldTwoArguments = listOf(
        CompiledArgument("color", "blue"),
        CompiledArgument("episode", "JEDI")
    )
    val fieldTwo = createCompiledField("hero", "hero", fieldTwoArguments)
    Truth.assertThat(cacheKeyBuilder.build(fieldTwo, variables)).isEqualTo(cacheKeyBuilder.build(field, variables))
  }

  @Test
  fun testFieldWithNestedObject() {
    val arguments = listOf(
        CompiledArgument("episode", "JEDI"),
        CompiledArgument("nested",
            mapOf<String, Any>(
                "foo" to 1,
                "bar" to 2
            )
        )
    )
    val field = createCompiledField("hero", "hero", arguments)
    val variables = Executable.Variables(emptyMap())

    Truth.assertThat(cacheKeyBuilder.build(field, variables)).isEqualTo("hero({\"episode\":\"JEDI\",\"nested\":{\"bar\":2,\"foo\":1}})")
  }

  @Test
  fun testFieldWithNonPrimitiveValue() {
    val field = CompiledField(
        type = CompiledStringType,
        name = "hero",
        arguments = listOf(CompiledArgument("episode", Episode.JEDI))
    )

    val variables = Executable.Variables(emptyMap())
    Truth.assertThat(cacheKeyBuilder.build(field, variables)).isEqualTo("hero({\"episode\":\"JEDI\"})")
  }

  @Test
  fun testFieldWithNestedObjectAndVariables() {
    val arguments = listOf(
        CompiledArgument("episode", "JEDI"),
        CompiledArgument("nested",
            mapOf(
                "foo" to CompiledVariable("stars"),
                "bar" to "2"
            )
        )
    )
    val field = createCompiledField("hero", "hero", arguments)
    val variables = Executable.Variables(mapOf("stars" to 1))

    Truth.assertThat(cacheKeyBuilder.build(field, variables)).isEqualTo("hero({\"episode\":\"JEDI\",\"nested\":{\"bar\":\"2\",\"foo\":1}})")
  }

  @Test
  fun fieldInputTypeArgument() {
    val arguments = listOf(
        CompiledArgument("episode", "JEDI"),
        CompiledArgument(
            "nested",
            mapOf(
                "foo" to CompiledVariable("testInput"),
                "bar" to "2"
            )
        )
    )
    val field = createCompiledField("hero", "hero", arguments)
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
    val arguments = listOf(
        CompiledArgument("episode", null),
        CompiledArgument("nested",
            mapOf(
                "foo" to CompiledVariable("testInput"),
                "bar" to null
            )
        )
    )
    val field = createCompiledField("hero", "hero", arguments)
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

  private fun createCompiledField(responseName: String, fieldName: String, arguments: List<CompiledArgument> = emptyList()): CompiledField {
    return CompiledField(
        type = CompiledStringType,
        name = fieldName,
        alias = responseName,
        arguments = arguments
    )
  }

  @Test
  fun testFieldWithVariablesInLists() {
    val arguments = listOf(
        CompiledArgument("where",
            mapOf(
                "and" to listOf(
                    CompiledVariable("stars")
                )
            )
        )
    )

    val field = createCompiledField("hero", "hero", arguments)
    val variables0 = Executable.Variables(mapOf("stars" to listOf(0)))
    val variables1 = Executable.Variables(mapOf("stars" to listOf(1)))

    Truth.assertThat(cacheKeyBuilder.build(field, variables0)).isNotEqualTo(cacheKeyBuilder.build(field, variables1))
  }
}
