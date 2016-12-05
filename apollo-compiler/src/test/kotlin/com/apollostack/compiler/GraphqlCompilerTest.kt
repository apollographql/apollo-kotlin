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
    compiler.write("src/test/data/HeroName.json")
    val outputFile = File("build/generated/source/apollo/test/HeroName.java")
    assertThat(outputFile.readText()).isEqualTo(
        """package test;

import java.lang.String;

public interface HeroName {
  Character hero();

  interface Character {
    String name();
  }
}
""")
    val source = JavaFileObjects.forSourceLines("test.HeroName", outputFile.readLines())
    assertAbout(javaSources())
        .that(listOf(source))
        .compilesWithoutError()
  }

  @Test fun twoHeroes() {
    compiler.write("src/test/data/TwoHeroes.json")
    val outputFile = File("build/generated/source/apollo/test/TwoHeroes.java")
    assertThat(outputFile.readText()).isEqualTo(
"""package test;

import java.lang.String;

public interface TwoHeroes {
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

public interface HeroDetails {
  Character hero();

  interface Character {
    String name();

    CharacterFriendsConnection friendsConnection();
  }

  interface CharacterFriendsConnection {
    int totalCount();

    CharacterFriendsConnectionFriendsEdge edges();
  }

  interface CharacterFriendsConnectionFriendsEdge {
    CharacterFriendsConnectionFriendsEdgeCharacter node();
  }

  interface CharacterFriendsConnectionFriendsEdgeCharacter {
    String name();
  }
}
""")

    val source = JavaFileObjects.forSourceLines("test.HeroDetails", outputFile.readLines())
    assertAbout(javaSources())
      .that(listOf(source))
      .compilesWithoutError()
  }

  @Test fun twoHeroesUnique() {
    compiler.write("src/test/data/TwoHeroesUnique.json")
    val outputFile = File("build/generated/source/apollo/test/TwoHeroesUnique.java")
    assertThat(outputFile.readText()).isEqualTo(
        """package test;

import java.lang.String;

public interface TwoHeroesUnique {
  R2Character r2();

  LukeCharacter luke();

  interface LukeCharacter {
    long id();

    String name();
  }

  interface R2Character {
    String name();
  }
}
""")
    val source = JavaFileObjects.forSourceLines("test.TwoHeroesUnique", outputFile.readLines())
    assertAbout(javaSources())
        .that(listOf(source))
        .compilesWithoutError()
  }
}