package com.apollostack.compiler

import com.google.common.truth.Truth.assertAbout
import com.google.common.truth.Truth.assertThat
import com.google.testing.compile.JavaFileObjects
import com.google.testing.compile.JavaSourcesSubjectFactory.javaSources
import org.junit.Test
import java.io.File

class GraphQLCompilerTest {
  private val compiler = GraphQLCompiler()

  @Test fun shouldThrowExceptionForInvalidIrPath() {
    val irFile = File("src/test/dummyfolder/graphql/com/example/HeroName.json")
    try {
      compiler.write(irFile)
    } catch (ex: IllegalArgumentException) {
      assertThat(ex.message.equals("Files must be organized like src/main/graphql/..."))
    }
  }

  @Test fun heroName() {
    val actual = actualFileFor("hero_name", "TestQuery")
    val expected = expectedFileFor("hero_name", "TestQuery")

    compiler.write(irFileFor("hero_name", "TestQuery"))
    assertThat(actual.readText()).isEqualTo(expected.readText())

    assertAbout(javaSources())
        .that(listOf(JavaFileObjects.forSourceLines("com.example.hero_name.TestQuery", actual.readLines())))
        .compilesWithoutError()
  }

  @Test fun twoHeroes() {
    val actual = actualFileFor("two_heroes", "TestQuery")
    val expected = expectedFileFor("two_heroes", "TestQuery")

    compiler.write(irFileFor("two_heroes", "TestQuery"))
    assertThat(actual.readText()).isEqualTo(expected.readText())

    assertAbout(javaSources())
        .that(listOf(JavaFileObjects.forSourceLines("com.example.two_heroes.TestQuery", actual.readLines())))
        .compilesWithoutError()
  }

  @Test fun heroDetails() {
    val actual = actualFileFor("hero_details", "TestQuery")
    val expected = expectedFileFor("hero_details", "TestQuery")

    compiler.write(irFileFor("hero_details", "TestQuery"))
    assertThat(actual.readText()).isEqualTo(expected.readText())

    assertAbout(javaSources())
        .that(listOf(JavaFileObjects.forSourceLines("com.example.hero_details.TestQuery", actual.readLines())))
        .compilesWithoutError()
  }

  @Test fun twoHeroesUnique() {
    val actual = actualFileFor("two_heroes_unique", "TestQuery")
    val expected = expectedFileFor("two_heroes_unique", "TestQuery")

    compiler.write(irFileFor("two_heroes_unique", "TestQuery"))
    assertThat(actual.readText()).isEqualTo(expected.readText())

    assertAbout(javaSources())
        .that(listOf(JavaFileObjects.forSourceLines("com.example.two_heroes_unique.TestQuery", actual.readLines())))
        .compilesWithoutError()
  }

  @Test fun scalarTypes() {
    val actual = actualFileFor("scalar_types", "TestQuery")
    val expected = expectedFileFor("scalar_types", "TestQuery")

    compiler.write(irFileFor("scalar_types", "TestQuery"))
    assertThat(actual.readText()).isEqualTo(expected.readText())

    val source = JavaFileObjects.forSourceLines("com.example.scalar_types.TestQuery", actual.readLines())
    assertAbout(javaSources())
        .that(listOf(source))
        .compilesWithoutError()
  }

  @Test fun enumType() {
    val actual = actualFileFor("enum_type", "TestQuery")
    val expected = expectedFileFor("enum_type", "TestQuery")
    val episodeEnumActual = actualFileFor("enum_type", "Episode")
    val episodeEnumExpected = expectedFileFor("enum_type", "Episode")

    compiler.write(irFileFor("enum_type", "TestQuery"))

    assertThat(actual.readText()).isEqualTo(expected.readText())
    assertThat(episodeEnumActual.readText()).isEqualTo(episodeEnumExpected.readText())

    val episodeEnumSource = JavaFileObjects.forSourceLines("com.example.enum_type.Episode",
        episodeEnumActual.readLines())
    val source = JavaFileObjects.forSourceLines("com.example.enum_type.TestQuery", actual.readLines())
    assertAbout(javaSources())
        .that(listOf(source, episodeEnumSource))
        .compilesWithoutError()
  }

  @Test fun simpleFragment() {
    val actual = actualFileFor("simple_fragment", "TestQuery")
    val expected = expectedFileFor("simple_fragment", "TestQuery")
    val fragmentActual = actualFileFor("simple_fragment", "HeroDetails")
    val fragmentExpected = expectedFileFor("simple_fragment", "HeroDetails")

    compiler.write(irFileFor("simple_fragment", "TestQuery"))

    assertThat(actual.readText()).isEqualTo(expected.readText())
    assertThat(fragmentActual.readText()).isEqualTo(fragmentExpected.readText())

    val fragment = JavaFileObjects.forSourceLines("com.example.simple_fragment.HeroDetails",
        fragmentActual.readLines())
    val source = JavaFileObjects.forSourceLines("com.example.simple_fragment.TestQuery", actual.readLines())
    assertAbout(javaSources())
        .that(listOf(source, fragment))
        .compilesWithoutError()
  }

  @Test fun fragmentFriendsConnection() {
    val actual = actualFileFor("fragment_friends_connection", "TestQuery")
    val expected = expectedFileFor("fragment_friends_connection", "TestQuery")
    val fragmentActual = actualFileFor("fragment_friends_connection", "HeroDetails")
    val fragmentExpected = expectedFileFor("fragment_friends_connection", "HeroDetails")

    compiler.write(irFileFor("fragment_friends_connection", "TestQuery"))

    assertThat(actual.readText()).isEqualTo(expected.readText())
    assertThat(fragmentActual.readText()).isEqualTo(fragmentExpected.readText())

    val fragment = JavaFileObjects.forSourceLines("com.example.fragment_friends_connection.HeroDetails",
        fragmentActual.readLines())
    val source = JavaFileObjects.forSourceLines("com.example.fragment_friends_connection.TestQuery",
        actual.readLines())
    assertAbout(javaSources())
        .that(listOf(source, fragment))
        .compilesWithoutError()
  }

  @Test fun simpleInlineFragments() {
    val actual = actualFileFor("simple_inline_fragment", "TestQuery")
    val expected = expectedFileFor("simple_inline_fragment", "TestQuery")

    compiler.write(irFileFor("simple_inline_fragment", "TestQuery"))
    assertThat(actual.readText()).isEqualTo(expected.readText())

    val source = JavaFileObjects.forSourceLines("com.example.TestQuery", actual.readLines())
    assertAbout(javaSources())
        .that(listOf(source))
        .compilesWithoutError()
  }

  @Test fun inlineFragmentsWithFriends() {
    val actual = actualFileFor("inline_fragments_with_friends", "TestQuery")
    val expected = expectedFileFor("inline_fragments_with_friends", "TestQuery")
    val episodeEnumActual = actualFileFor("inline_fragments_with_friends", "Episode")

    compiler.write(irFileFor("inline_fragments_with_friends", "TestQuery"))
    assertThat(actual.readText()).isEqualTo(expected.readText())

    val episodeEnumSource = JavaFileObjects.forSourceLines("test.Episode",
        episodeEnumActual.readLines())
    val source = JavaFileObjects.forSourceLines("com.example.TestQuery", actual.readLines())
    assertAbout(javaSources())
        .that(listOf(source, episodeEnumSource))
        .compilesWithoutError()
  }

  @Test fun fragmentsWithTypeCondition() {
    val actual = actualFileFor("fragments_with_type_condition", "TestQuery")
    val expected = expectedFileFor("fragments_with_type_condition", "TestQuery")
    val humanDetailsActual = actualFileFor("fragments_with_type_condition", "HumanDetails")
    val droidDetailsActual = actualFileFor("fragments_with_type_condition", "DroidDetails")

    compiler.write(irFileFor("fragments_with_type_condition", "TestQuery"))
    assertThat(actual.readText()).isEqualTo(expected.readText())

    val humanDetails = JavaFileObjects.forSourceLines("test.HumanDetails",
        humanDetailsActual.readLines())
    val droidDetails = JavaFileObjects.forSourceLines("test.DroidDetails",
        droidDetailsActual.readLines())
    val source = JavaFileObjects.forSourceLines("com.example.TestQuery", actual.readLines())
    assertAbout(javaSources())
        .that(listOf(source, humanDetails, droidDetails))
        .compilesWithoutError()
  }

  @Test fun uniqueTypeName() {
    val actual = actualFileFor("unique_type_name", "TestQuery")
    val expected = expectedFileFor("unique_type_name", "TestQuery")
    val heroDetailsActual = actualFileFor("unique_type_name", "HeroDetails")
    val episodeActual = actualFileFor("unique_type_name", "Episode")

    compiler.write(irFileFor("unique_type_name", "TestQuery"))
    assertThat(actual.readText()).isEqualTo(expected.readText())

    val heroDetails = JavaFileObjects.forSourceLines("com.example.unique_type_name.HeroDetails",
        heroDetailsActual.readLines())

    val episode = JavaFileObjects.forSourceLines("com.example.unique_type_name.Episode",
        episodeActual.readLines())

    val source = JavaFileObjects.forSourceLines("com.example.unique_type_name.TestQuery", actual.readLines())
    assertAbout(javaSources())
        .that(listOf(source, heroDetails, episode))
        .compilesWithoutError()
  }

  @Test fun fieldDirectives() {
    val actualFile = actualFileFor("directives", "TestQuery")
    val expectedFile = expectedFileFor("directives", "TestQuery")

    compiler.write(irFileFor("directives", "TestQuery"))
    assertThat(actualFile.readText()).isEqualTo(expectedFile.readText())
  }

  @Test fun fragmentsWithInlineFragment() {
    val actual = actualFileFor("fragment_with_inline_fragment", "TestQuery")
    val expected = expectedFileFor("fragment_with_inline_fragment", "TestQuery")
    val heroDetailsActual = actualFileFor("fragment_with_inline_fragment", "HeroDetails")
    val heroDetailsExpected = expectedFileFor("fragment_with_inline_fragment", "HeroDetails")
    val episodeActual = actualFileFor("fragment_with_inline_fragment", "Episode")

    compiler.write(irFileFor("fragment_with_inline_fragment", "TestQuery"))
    assertThat(actual.readText()).isEqualTo(expected.readText())
    assertThat(heroDetailsActual.readText()).isEqualTo(heroDetailsExpected.readText())

    val heroDetails = JavaFileObjects.forSourceLines("com.example.fragment_with_inline_fragment.HeroDetails",
        heroDetailsActual.readLines())
    val episode = JavaFileObjects.forSourceLines("com.example.fragment_with_inline_fragment.Episode",
        episodeActual.readLines())
    val source = JavaFileObjects.forSourceLines("com.example.fragment_with_inline_fragment.TestQuery",
        actual.readLines())
    assertAbout(javaSources())
        .that(listOf(source, heroDetails, episode))
        .compilesWithoutError()
  }

  @Test fun simpleArguments() {
    val actual = actualFileFor("simple_arguments", "TestQuery")
    val expected = expectedFileFor("simple_arguments", "TestQuery")
    val episodeActual = actualFileFor("simple_arguments", "Episode")

    compiler.write(irFileFor("simple_arguments", "TestQuery"))
    assertThat(actual.readText()).isEqualTo(expected.readText())

    val episode = JavaFileObjects.forSourceLines("com.example.simple_arguments.Episode",
        episodeActual.readLines())
    val source = JavaFileObjects.forSourceLines("com.example.simple_arguments.TestQuery",
        actual.readLines())
    assertAbout(javaSources())
        .that(listOf(source, episode))
        .compilesWithoutError()
  }
}