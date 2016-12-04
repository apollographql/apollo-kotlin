package com.apollostack.compiler

import com.google.common.truth.Truth.assertAbout
import com.google.common.truth.Truth.assertThat
import com.google.testing.compile.JavaFileObjects
import com.google.testing.compile.JavaSourcesSubjectFactory.javaSources
import org.junit.Test
import java.io.File

class GraphqlCompilerTest {
  @Test fun heroName() {
    val compiler = GraphqlCompiler()
    compiler.write("src/test/data/HeroName.json")
    val outputFile = File("build/generated/source/apollo/test/HeroName.java")
    assertThat(outputFile.readText()).isEqualTo(
        """package test;

import java.lang.String;

interface HeroName {
  Character hero();

  interface Character {
    String name();
  }
}
""")
    val source = JavaFileObjects.forSourceLines("test.TwoHeroes", outputFile.readLines())
    assertAbout(javaSources())
        .that(listOf(source))
        .compilesWithoutError()
  }

  @Test fun twoHeroes() {
    val compiler = GraphqlCompiler()
    compiler.write("src/test/data/TwoHeroes.json")
    val outputFile = File("build/generated/source/apollo/test/TwoHeroes.java")
    assertThat(outputFile.readText()).isEqualTo(
"""package test;

import java.lang.String;

interface TwoHeroes {
  Character r2();

  Character luke();

  interface Character {
    String name();
  }
}
""")
    val source = JavaFileObjects.forSourceLines("test.TwoHeroes", outputFile.readLines())
    assertAbout(javaSources())
        .that(listOf(source))
        .compilesWithoutError()
  }

  @Test fun heroDetails() {
    val compiler = GraphqlCompiler()
    compiler.write("src/test/data/HeroDetails.json")
    val outputFile = File("build/generated/source/apollo/test/HeroDetails.java")
    assertThat(outputFile.readText()).isEqualTo(
      """package test;

import java.lang.String;
import java.util.List;

interface HeroDetails {
  Character hero();

  interface Character {
    String name();

    FriendsConnection friendsConnection();

    interface FriendsConnection {
      int totalCount();

      List<FriendsEdge> edges();

      interface FriendsEdge {
        Character node();

        interface Character {
          String name();
        }
      }
    }
  }
}
""")

    val source = JavaFileObjects.forSourceLines("test.HeroDetails", outputFile.readLines())
    assertAbout(javaSources())
      .that(listOf(source))
      .compilesWithoutError()
  }
}