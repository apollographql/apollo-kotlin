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
    val queryFile = File("src/test/graphql/com/apollostack/compiler/query/HeroName.json")
    val actualFile = File("build/generated/source/apollo/com/apollostack/compiler/query/HeroName.java")
    val expectedFile = File("src/test/graphql/com/apollostack/compiler/query/HeroNameExpected.java")

    compiler.write(queryFile)
    assertThat(actualFile.readText()).isEqualTo(expectedFile.readText())

    val source = JavaFileObjects.forSourceLines("com.apollostack.compiler.query.HeroName", actualFile.readLines())
    assertAbout(javaSources())
      .that(listOf(source))
      .compilesWithoutError()
  }

  @Test fun twoHeroes() {
    val queryFile = File("src/test/graphql/com/apollostack/compiler/query/TwoHeroes.json")
    val actualFile = File("build/generated/source/apollo/com/apollostack/compiler/query/TwoHeroes.java")
    val expectedFile = File("src/test/graphql/com/apollostack/compiler/query/TwoHeroesExpected.java")

    compiler.write(queryFile)
    assertThat(actualFile.readText()).isEqualTo(expectedFile.readText())

    val source = JavaFileObjects.forSourceLines("com.apollostack.compiler.query.TwoHeroes", actualFile.readLines())
    assertAbout(javaSources())
      .that(listOf(source))
      .compilesWithoutError()
  }

  @Test fun heroDetails() {
    val queryFile = File("src/test/graphql/com/apollostack/compiler/query/HeroDetails.json")
    val actualFile = File("build/generated/source/apollo/com/apollostack/compiler/query/HeroDetails.java")
    val expectedFile = File("src/test/graphql/com/apollostack/compiler/query/HeroDetailsExpected.java")

    compiler.write(queryFile)
    assertThat(actualFile.readText()).isEqualTo(expectedFile.readText())

    val source = JavaFileObjects.forSourceLines("com.apollostack.compiler.query.HeroDetails", actualFile.readLines())
    assertAbout(javaSources())
      .that(listOf(source))
      .compilesWithoutError()
  }

  @Test fun twoHeroesUnique() {
    val queryFile = File("src/test/graphql/com/apollostack/compiler/query/TwoHeroesUnique.json")
    val actualFile = File("build/generated/source/apollo/com/apollostack/compiler/query/TwoHeroesUnique.java")
    val expectedFile = File("src/test/graphql/com/apollostack/compiler/query/TwoHeroesUniqueExpected.java")

    compiler.write(queryFile)
    assertThat(actualFile.readText()).isEqualTo(expectedFile.readText())

    val source = JavaFileObjects.forSourceLines("com.apollostack.compiler.query.TwoHeroesUnique", actualFile.readLines())
    assertAbout(javaSources())
      .that(listOf(source))
      .compilesWithoutError()
  }

  @Test fun graphQlScalarTypes() {
    val queryFile = File("src/test/graphql/com/apollostack/compiler/query/ScalarTypes.json")
    val actualFile = File("build/generated/source/apollo/com/apollostack/compiler/query/ScalarTypes.java")
    val expectedFile = File("src/test/graphql/com/apollostack/compiler/query/ScalarTypesExpected.java")

    compiler.write(queryFile)
    assertThat(actualFile.readText()).isEqualTo(expectedFile.readText())

    val source = JavaFileObjects.forSourceLines("com.apollostack.compiler.query.ScalarTypes", actualFile.readLines())
    assertAbout(javaSources())
      .that(listOf(source))
      .compilesWithoutError()
  }
}