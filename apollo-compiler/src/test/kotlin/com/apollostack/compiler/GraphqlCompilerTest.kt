package com.apollostack.compiler

import com.google.common.truth.Truth.assertAbout
import com.google.common.truth.Truth.assertThat
import com.google.testing.compile.JavaFileObjects
import com.google.testing.compile.JavaSourcesSubjectFactory.javaSources
import org.junit.Test
import java.io.File

class GraphqlCompilerTest {
  private val compiler = GraphqlCompiler()

  @Test fun shouldThrowExceptionForInvalidIrPath() {
    val irFile = File("src/test/dummyfolder/graphql/com/example/HeroName.json")
    try {
      compiler.write(irFile)
    } catch (ex: IllegalArgumentException) {
      assertThat(ex.message.equals("Files must be organized like src/main/graphql/..."))
    }
  }

  @Test fun heroName() {
    val actual = actualFileFor("hero_name", "HeroName")
    val expected = expectedFileFor("hero_name", "HeroName")

    compiler.write(irFileFor("hero_name", "HeroName"))
    assertThat(actual.readText()).isEqualTo(expected.readText())

    assertAbout(javaSources())
        .that(listOf(JavaFileObjects.forSourceLines("test.HeroName", actual.readLines())))
        .compilesWithoutError()
  }

  @Test fun twoHeroes() {
    val actual = actualFileFor("two_heroes", "TwoHeroes")
    val expected = expectedFileFor("two_heroes", "TwoHeroes")

    compiler.write(irFileFor("two_heroes", "TwoHeroes"))
    assertThat(actual.readText()).isEqualTo(expected.readText())

    assertAbout(javaSources())
        .that(listOf(JavaFileObjects.forSourceLines("test.TwoHeroes", actual.readLines())))
        .compilesWithoutError()
  }

  @Test fun heroDetails() {
    val actual = actualFileFor("hero_details", "HeroDetails")
    val expected = expectedFileFor("hero_details", "HeroDetails")

    compiler.write(irFileFor("hero_details", "HeroDetails"))
    assertThat(actual.readText()).isEqualTo(expected.readText())

    assertAbout(javaSources())
        .that(listOf(JavaFileObjects.forSourceLines("test.HeroDetails", actual.readLines())))
        .compilesWithoutError()
  }

  @Test fun twoHeroesUnique() {
    val actual = actualFileFor("two_heroes_unique", "TwoHeroesUnique")
    val expected = expectedFileFor("two_heroes_unique", "TwoHeroesUnique")

    compiler.write(irFileFor("two_heroes_unique", "TwoHeroesUnique"))
    assertThat(actual.readText()).isEqualTo(expected.readText())

    assertAbout(javaSources())
        .that(listOf(JavaFileObjects.forSourceLines("test.TwoHeroesUnique", actual.readLines())))
        .compilesWithoutError()
  }

  @Test fun scalarTypes() {
    val actual = actualFileFor("scalar_types", "ScalarTypes")
    val expected = expectedFileFor("scalar_types", "ScalarTypes")

    compiler.write(irFileFor("scalar_types", "ScalarTypes"))
    assertThat(actual.readText()).isEqualTo(expected.readText())

    val source = JavaFileObjects.forSourceLines("test.ScalarTypes", actual.readLines())
    assertAbout(javaSources())
        .that(listOf(source))
        .compilesWithoutError()
  }

  @Test fun enumType() {
    val actual = actualFileFor("enum_type", "HeroAppearsIn")
    val expected = expectedFileFor("enum_type", "HeroAppearsIn")
    val episodeEnumActual = actualFileFor("enum_type", "Episode")
    val episodeEnumExpected = expectedFileFor("enum_type", "Episode")

    compiler.write(irFileFor("enum_type", "HeroAppearsIn"))

    assertThat(actual.readText()).isEqualTo(expected.readText())
    assertThat(episodeEnumActual.readText()).isEqualTo(episodeEnumExpected.readText())

    val episodeEnumSource = JavaFileObjects.forSourceLines("test.Episode",
        episodeEnumActual.readLines())
    val source = JavaFileObjects.forSourceLines("test.HeroAppearsIn", actual.readLines())
    assertAbout(javaSources())
        .that(listOf(source, episodeEnumSource))
        .compilesWithoutError()
  }

  @Test fun simpleFragment() {
    val actual = actualFileFor("simple_fragment", "SimpleFragment")
    val expected = expectedFileFor("simple_fragment", "SimpleFragment")
    val fragmentActual = actualFileFor("simple_fragment", "HeroDetails")
    val fragmentExpected = expectedFileFor("simple_fragment", "HeroDetails")

    compiler.write(irFileFor("simple_fragment", "SimpleFragment"))

    assertThat(actual.readText()).isEqualTo(expected.readText())
    assertThat(fragmentActual.readText()).isEqualTo(fragmentExpected.readText())

    val fragment = JavaFileObjects.forSourceLines("test.HeroDetails",
        fragmentActual.readLines())
    val source = JavaFileObjects.forSourceLines("test.SimpleFragment", actual.readLines())
    assertAbout(javaSources())
        .that(listOf(source, fragment))
        .compilesWithoutError()
  }

  @Test fun fragmentFriendsConnection() {
    val actual = actualFileFor("fragment_friends_connection", "FragmentFriendsConnection")
    val expected = expectedFileFor("fragment_friends_connection", "FragmentFriendsConnection")
    val fragmentActual = actualFileFor("fragment_friends_connection", "HeroDetails")
    val fragmentExpected = expectedFileFor("fragment_friends_connection", "HeroDetails")

    compiler.write(irFileFor("fragment_friends_connection", "FragmentFriendsConnection"))

    assertThat(actual.readText()).isEqualTo(expected.readText())
    assertThat(fragmentActual.readText()).isEqualTo(fragmentExpected.readText())

    val fragment = JavaFileObjects.forSourceLines("test.HeroDetails",
        fragmentActual.readLines())
    val source = JavaFileObjects.forSourceLines("test.FragmentFriendsConnection",
        actual.readLines())
    assertAbout(javaSources())
        .that(listOf(source, fragment))
        .compilesWithoutError()
  }

  @Test fun simpleInlineFragments() {
    val actual = actualFileFor("simple_inline_fragment", "Query")
    val expected = expectedFileFor("simple_inline_fragment", "Query")

    compiler.write(irFileFor("simple_inline_fragment", "Query"))
    assertThat(actual.readText()).isEqualTo(expected.readText())

    val source = JavaFileObjects.forSourceLines("com.example.Query", actual.readLines())
    assertAbout(javaSources())
        .that(listOf(source))
        .compilesWithoutError()
  }

  @Test fun inlineFragmentsWithFriends() {
    val actual = actualFileFor("inline_fragments_with_friends", "Query")
    val expected = expectedFileFor("inline_fragments_with_friends", "Query")
    val episodeEnumActual = actualFileFor("inline_fragments_with_friends", "Episode")

    compiler.write(irFileFor("inline_fragments_with_friends", "Query"))
    assertThat(actual.readText()).isEqualTo(expected.readText())

    val episodeEnumSource = JavaFileObjects.forSourceLines("test.Episode",
        episodeEnumActual.readLines())
    val source = JavaFileObjects.forSourceLines("com.example.Query", actual.readLines())
    assertAbout(javaSources())
        .that(listOf(source, episodeEnumSource))
        .compilesWithoutError()
  }

  @Test fun fragmentsWithTypeCondition() {
    val actual = actualFileFor("fragments_with_type_condition", "Query")
    val expected = expectedFileFor("fragments_with_type_condition", "Query")
    val humanDetailsActual = actualFileFor("fragments_with_type_condition", "HumanDetails")
    val droidDetailsActual = actualFileFor("fragments_with_type_condition", "DroidDetails")

    compiler.write(irFileFor("fragments_with_type_condition", "Query"))
    assertThat(actual.readText()).isEqualTo(expected.readText())

    val humanDetails = JavaFileObjects.forSourceLines("test.HumanDetails",
        humanDetailsActual.readLines())
    val droidDetails = JavaFileObjects.forSourceLines("test.DroidDetails",
        droidDetailsActual.readLines())
    val source = JavaFileObjects.forSourceLines("com.example.Query", actual.readLines())
    assertAbout(javaSources())
        .that(listOf(source, humanDetails, droidDetails))
        .compilesWithoutError()
  }

  @Test fun uniqueName() {
    val actual = actualFileFor("unique_type_name", "Query")
    val expected = expectedFileFor("unique_type_name", "Query")
    val heroDetailsActual = actualFileFor("unique_type_name", "HeroDetails")
    val episodeActual = actualFileFor("unique_type_name", "Episode")

    compiler.write(irFileFor("unique_type_name", "QueryIR"))
    assertThat(actual.readText()).isEqualTo(expected.readText())

    val heroDetails = JavaFileObjects.forSourceLines("com.example.unique_type_name.HeroDetails",
        heroDetailsActual.readLines())

    val episode = JavaFileObjects.forSourceLines("com.example.unique_type_name.Episode",
        episodeActual.readLines())

    val source = JavaFileObjects.forSourceLines("com.example.unique_type_name.Query", actual.readLines())
    assertAbout(javaSources())
        .that(listOf(source, heroDetails, episode))
        .compilesWithoutError()
  }

  @Test fun fieldDirectives() {
    val actualFile = actualFileFor("directives", "HeroNameDirective")
    val expectedFile = expectedFileFor("directives", "HeroNameDirective")

    compiler.write(irFileFor("directives", "HeroNameDirective"))
    assertThat(actualFile.readText()).isEqualTo(expectedFile.readText())
  }
}