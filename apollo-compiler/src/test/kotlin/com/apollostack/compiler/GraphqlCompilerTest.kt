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
    val irFile = File("src/test/graphql/com/example/HeroName.json")
    val actualFile = File("build/generated/source/apollo/com/example/HeroName.java")
    val expectedFile = File("src/test/graphql/com/example/HeroNameExpected.java")

    compiler.write(irFile)
    assertThat(actualFile.readText()).isEqualTo(expectedFile.readText())

    val source = JavaFileObjects.forSourceLines("com.apollostack.compiler.query.HeroName", actualFile.readLines())
    assertAbout(javaSources())
      .that(listOf(source))
      .compilesWithoutError()
  }

  @Test fun twoHeroes() {
    val irFile = File("src/test/graphql/com/example/TwoHeroes.json")
    val actualFile = File("build/generated/source/apollo/com/example/TwoHeroes.java")
    val expectedFile = File("src/test/graphql/com/example/TwoHeroesExpected.java")

    compiler.write(irFile)
    assertThat(actualFile.readText()).isEqualTo(expectedFile.readText())

    val source = JavaFileObjects.forSourceLines("com.apollostack.compiler.query.TwoHeroes", actualFile.readLines())
    assertAbout(javaSources())
      .that(listOf(source))
      .compilesWithoutError()
  }

  @Test fun heroDetails() {
    val irFile = File("src/test/graphql/com/example/HeroDetails.json")
    val actualFile = File("build/generated/source/apollo/com/example/HeroDetails.java")
    val expectedFile = File("src/test/graphql/com/example/HeroDetailsExpected.java")

    compiler.write(irFile)
    assertThat(actualFile.readText()).isEqualTo(expectedFile.readText())

    val source = JavaFileObjects.forSourceLines("com.apollostack.compiler.query.HeroDetails", actualFile.readLines())
    assertAbout(javaSources())
      .that(listOf(source))
      .compilesWithoutError()
  }

  @Test fun twoHeroesUnique() {
    val irFile = File("src/test/graphql/com/example/TwoHeroesUnique.json")
    val actualFile = File("build/generated/source/apollo/com/example/TwoHeroesUnique.java")
    val expectedFile = File("src/test/graphql/com/example/TwoHeroesUniqueExpected.java")

    compiler.write(irFile)
    assertThat(actualFile.readText()).isEqualTo(expectedFile.readText())

    val source = JavaFileObjects.forSourceLines("com.apollostack.compiler.query.TwoHeroesUnique", actualFile.readLines())
    assertAbout(javaSources())
      .that(listOf(source))
      .compilesWithoutError()
  }

  @Test fun graphQlScalarTypes() {
    val irFile = File("src/test/graphql/com/example/ScalarTypes.json")
    val actualFile = File("build/generated/source/apollo/com/example/ScalarTypes.java")
    val expectedFile = File("src/test/graphql/com/example/ScalarTypesExpected.java")

    compiler.write(irFile)
    assertThat(actualFile.readText()).isEqualTo(expectedFile.readText())

    val source = JavaFileObjects.forSourceLines("com.apollostack.compiler.query.ScalarTypes", actualFile.readLines())
    assertAbout(javaSources())
      .that(listOf(source))
      .compilesWithoutError()
  }
}