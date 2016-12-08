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
    val actual = File("build/generated/source/apollo/com/example/hero_name/HeroName.java")
    val expected = File("src/test/graphql/com/example/hero_name/HeroNameExpected.java")

    compiler.write(File("src/test/graphql/com/example/hero_name/HeroName.json"))
    assertThat(actual.readText()).isEqualTo(expected.readText())

    assertAbout(javaSources())
        .that(listOf(JavaFileObjects.forSourceLines("test.HeroName", actual.readLines())))
        .compilesWithoutError()
  }

  @Test fun twoHeroes() {
    val actual = File("build/generated/source/apollo/com/example/two_heroes/TwoHeroes.java")
    val expected = File("src/test/graphql/com/example/two_heroes/TwoHeroesExpected.java")

    compiler.write(File("src/test/graphql/com/example/two_heroes/TwoHeroes.json"))
    assertThat(actual.readText()).isEqualTo(expected.readText())

    assertAbout(javaSources())
        .that(listOf(JavaFileObjects.forSourceLines("test.TwoHeroes", actual.readLines())))
        .compilesWithoutError()
  }

  @Test fun heroDetails() {
    val actual = File("build/generated/source/apollo/com/example/hero_details/HeroDetails.java")
    val expected = File("src/test/graphql/com/example/hero_details/HeroDetailsExpected.java")

    compiler.write(File("src/test/graphql/com/example/hero_details/HeroDetails.json"))
    assertThat(actual.readText()).isEqualTo(expected.readText())

    assertAbout(javaSources())
        .that(listOf(JavaFileObjects.forSourceLines("test.HeroDetails", actual.readLines())))
        .compilesWithoutError()
  }

  @Test fun twoHeroesUnique() {
    val actual = File(
        "build/generated/source/apollo/com/example/two_heroes_unique/TwoHeroesUnique.java")
    val expected = File(
        "src/test/graphql/com/example/two_heroes_unique/TwoHeroesUniqueExpected.java")

    compiler.write(File("src/test/graphql/com/example/two_heroes_unique/TwoHeroesUnique.json"))
    assertThat(actual.readText()).isEqualTo(expected.readText())

    assertAbout(javaSources())
        .that(listOf(JavaFileObjects.forSourceLines("test.TwoHeroesUnique", actual.readLines())))
        .compilesWithoutError()
  }

  @Test fun scalarTypes() {
    val actual = File("build/generated/source/apollo/com/example/scalar_types/ScalarTypes.java")
    val expected = File("src/test/graphql/com/example/scalar_types/ScalarTypesExpected.java")

    compiler.write(File("src/test/graphql/com/example/scalar_types/ScalarTypes.json"))
    assertThat(actual.readText()).isEqualTo(expected.readText())

    val source = JavaFileObjects.forSourceLines("test.ScalarTypes", actual.readLines())
    assertAbout(javaSources())
        .that(listOf(source))
        .compilesWithoutError()
  }

  @Test fun enumType() {
    val actual = File("build/generated/source/apollo/com/example/enum_type/HeroAppearsIn.java")
    val expected = File("src/test/graphql/com/example/enum_type/HeroAppearsInExpected.java")
    val episodeEnumActual = File("build/generated/source/apollo/com/example/enum_type/Episode.java")
    val episodeEnumExpected = File("src/test/graphql/com/example/enum_type/EpisodeExpected.java")

    compiler.write(File("src/test/graphql/com/example/enum_type/HeroAppearsIn.json"))

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
    val actual = File(
        "build/generated/source/apollo/com/example/simple_fragment/SimpleFragment.java")
    val expected = File("src/test/graphql/com/example/simple_fragment/SimpleFragmentExpected.java")
    val fragmentActual = File(
        "build/generated/source/apollo/com/example/simple_fragment/HeroDetails.java")
    val fragmentExpected = File(
        "src/test/graphql/com/example/simple_fragment/HeroDetailsExpected.java")

    compiler.write(File("src/test/graphql/com/example/simple_fragment/SimpleFragment.json"))

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
    val actual = File("build/generated/source/apollo/com/example/fragment_friends_connection/" +
        "FragmentFriendsConnection.java")
    val expected = File("src/test/graphql/com/example/fragment_friends_connection/" +
        "FragmentFriendsConnectionExpected.java")
    val fragmentActual = File(
        "build/generated/source/apollo/com/example/fragment_friends_connection/HeroDetails.java")
    val fragmentExpected = File(
        "src/test/graphql/com/example/fragment_friends_connection/HeroDetailsExpected.java")

    compiler.write(File(
        "src/test/graphql/com/example/fragment_friends_connection/FragmentFriendsConnection.json"))

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
}