package com.apollostack.compiler

import com.google.common.truth.Truth.assertAbout
import com.google.common.truth.Truth.assertThat
import com.google.testing.compile.JavaFileObjects
import com.google.testing.compile.JavaSourcesSubjectFactory.javaSources
import org.junit.Test
import java.io.File

class GraphqlCompilerTest {
  private val compiler = GraphqlCompiler()

  @Test fun heroName() {
    val actual = File("build/generated/source/apollo/com/example/HeroName.java")
    val expected = File("src/test/graphql/com/example/HeroNameExpected.java")

    compiler.write(File("src/test/graphql/com/example/HeroName.json"))
    assertThat(actual.readText()).isEqualTo(expected.readText())

    val source = JavaFileObjects.forSourceLines("com.example.HeroName", actual.readLines())
    assertAbout(javaSources())
        .that(listOf(source))
        .compilesWithoutError()
  }

  @Test fun twoHeroes() {
    val actual = File("build/generated/source/apollo/com/example/TwoHeroes.java")
    val expected = File("src/test/graphql/com/example/TwoHeroesExpected.java")

    compiler.write(File("src/test/graphql/com/example/TwoHeroes.json"))
    assertThat(actual.readText()).isEqualTo(expected.readText())

    val source = JavaFileObjects.forSourceLines("com.example.TwoHeroes", actual.readLines())
    assertAbout(javaSources())
        .that(listOf(source))
        .compilesWithoutError()
  }

  @Test fun heroDetails() {
    val actual = File("build/generated/source/apollo/com/example/HeroDetails.java")
    val expected = File("src/test/graphql/com/example/HeroDetailsExpected.java")

    compiler.write(File("src/test/graphql/com/example/HeroDetails.json"))
    assertThat(actual.readText()).isEqualTo(expected.readText())

    val source = JavaFileObjects.forSourceLines("com.example.HeroDetails", actual.readLines())
    assertAbout(javaSources())
        .that(listOf(source))
        .compilesWithoutError()
  }

  @Test fun twoHeroesUnique() {
    val actual = File("build/generated/source/apollo/com/example/TwoHeroesUnique.java")
    val expected = File("src/test/graphql/com/example/TwoHeroesUniqueExpected.java")

    compiler.write(File("src/test/graphql/com/example/TwoHeroesUnique.json"))
    assertThat(actual.readText()).isEqualTo(expected.readText())

    val source = JavaFileObjects.forSourceLines("com.example.TwoHeroesUnique",
        actual.readLines())
    assertAbout(javaSources())
        .that(listOf(source))
        .compilesWithoutError()
  }

  @Test fun graphQlScalarTypes() {
    val actual = File("build/generated/source/apollo/com/example/ScalarTypes.java")
    val expected = File("src/test/graphql/com/example/ScalarTypesExpected.java")

    compiler.write(File("src/test/graphql/com/example/ScalarTypes.json"))
    assertThat(actual.readText()).isEqualTo(expected.readText())

    val source = JavaFileObjects.forSourceLines("com.example.ScalarTypes", actual.readLines())
    assertAbout(javaSources())
        .that(listOf(source))
        .compilesWithoutError()
  }

  @Test fun enumType() {
    val actual = File("build/generated/source/apollo/com/example/HeroAppearsIn.java")
    val expected = File("src/test/graphql/com/example/HeroAppearsInExpected.java")
    val episodeEnumActual = File("build/generated/source/apollo/com/example/Episode.java")
    val episodeEnumExpected = File("src/test/graphql/com/example/EpisodeExpected.java")

    compiler.write(File("src/test/graphql/com/example/HeroAppearsIn.json"))

    assertThat(actual.readText()).isEqualTo(expected.readText())
    assertThat(episodeEnumActual.readText()).isEqualTo(episodeEnumExpected.readText())

    val episodeEnumSource = JavaFileObjects.forSourceLines("com.example.Episode",
        episodeEnumActual.readLines())
    val source = JavaFileObjects.forSourceLines("com.example.HeroAppearsIn", actual.readLines())
    assertAbout(javaSources())
        .that(listOf(source, episodeEnumSource))
        .compilesWithoutError()
  }

  @Test fun simpleFragment() {
    val actual = File("build/generated/source/apollo/com/example/SimpleFragment.java")
    val expected = File("src/test/graphql/com/example/SimpleFragmentExpected.java")
    val fragmentActual = File("build/generated/source/apollo/com/example/HeroDetailsFragment.java")
    val fragmentExpected = File("src/test/graphql/com/example/HeroDetailsFragmentExpected.java")

    compiler.write(File("src/test/graphql/com/example/SimpleFragment.json"))

    assertThat(actual.readText()).isEqualTo(expected.readText())
    assertThat(fragmentActual.readText()).isEqualTo(fragmentExpected.readText())

    val fragment = JavaFileObjects.forSourceLines("com.example.HeroDetailsFragment",
        fragmentActual.readLines())
    val source = JavaFileObjects.forSourceLines("com.example.SimpleFragment", actual.readLines())
    assertAbout(javaSources())
        .that(listOf(source, fragment))
        .compilesWithoutError()
  }
}