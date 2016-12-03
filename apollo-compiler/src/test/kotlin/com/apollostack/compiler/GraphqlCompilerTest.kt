package com.apollostack.compiler

import com.google.common.truth.Truth.assertAbout
import com.google.common.truth.Truth.assertThat
import com.google.testing.compile.JavaFileObjects
import com.google.testing.compile.JavaSourcesSubjectFactory.javaSources
import org.junit.Test
import java.io.File

class GraphqlCompilerTest {
  @Test fun simpleQuery() {
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
}