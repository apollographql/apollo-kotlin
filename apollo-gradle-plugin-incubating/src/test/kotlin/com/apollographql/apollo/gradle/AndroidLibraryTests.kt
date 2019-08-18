package com.apollographql.apollo.gradle

import com.apollographql.apollo.gradle.util.TestUtils.withTestDirectory
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.Test
import java.io.File

class AndroidLibraryTests {
  @Test
  fun test() {
    withTestDirectory("androidLibrary") { dir ->
      `builds successfully and generates expected outputs`(dir)
      `nothing changed, generate classes up to date`(dir)
      `adding a custom type to the build script re-generates the CustomType class`(dir)
    }
  }

  private fun `builds successfully and generates expected outputs`(dir: File) {
    val result = GradleRunner.create()
        .withPluginClasspath()
        .forwardStdOutput(System.out.writer())
        .forwardStdError(System.err.writer())
        .withProjectDir(dir)
        .withArguments("build", "--stacktrace")
        .build()

    assert(result.task(":build")!!.outcome == TaskOutcome.SUCCESS)


    // Java classes generated successfully
    assert(File(dir, "build/generated/source/apollo/debug/0/com/example/DroidDetailsQuery.java").isFile)
    assert(File(dir, "build/generated/source/apollo/debug/0/com/example/FilmsQuery.java").isFile)
    assert(File(dir, "build/generated/source/apollo/debug/0/com/example/fragment/SpeciesInformation.java").isFile)

    // verify that the custom type generated was Object.class because no customType mapping was specified
    assert(File(dir, "build/generated/source/apollo/debug/0/com/example/type/CustomType.java").isFile)
    assert(File(dir, "build/generated/source/apollo/debug/0/com/example/type/CustomType.java")
        .readText()
        .contains("return Object.class;"))

    // Optional is not added to the generated classes
    assert(!File(dir, "build/generated/source/apollo/debug/0/com/example/DroidDetailsQuery.java")
        .readText()
        .contains("Optional"))
    assert(File(dir, "build/generated/source/apollo/debug/0/com/example/DroidDetailsQuery.java")
        .readText()
        .contains("import org.jetbrains.annotations.Nullable;"))
  }

  fun `nothing changed, generate classes up to date`(dir: File) {
    val result = GradleRunner.create()
        .withPluginClasspath()
        .forwardStdOutput(System.out.writer())
        .forwardStdError(System.err.writer())
        .withProjectDir(dir)
        .withArguments("generateApolloClasses", "--stacktrace")
        .build()

    assert(result.task(":generateApolloClasses")!!.outcome == TaskOutcome.UP_TO_DATE)

    // Java classes generated successfully
    assert(File(dir, "build/generated/source/apollo/debug/0/com/example/DroidDetailsQuery.java").isFile)
    assert(File(dir, "build/generated/source/apollo/debug/0/com/example/FilmsQuery.java").isFile)
    assert(File(dir, "build/generated/source/apollo/debug/0/com/example/fragment/SpeciesInformation.java").isFile)
  }

  fun `adding a custom type to the build script re-generates the CustomType class`(dir: File) {
    val apolloBlock = """
      apollo {
        customTypeMapping = ["DateTime": "java.util.Date"]
      }
    """.trimIndent()

    File(dir, "build.gradle").appendText(apolloBlock)

    val result = GradleRunner.create()
        .withPluginClasspath()
        .forwardStdOutput(System.out.writer())
        .forwardStdError(System.err.writer())
        .withProjectDir(dir)
        .withArguments("generateApolloClasses", "--stacktrace")
        .build()


    // modifying the customTypeMapping should cause the task to be out of date
    // and the task should run again
    assert(result.task(":generateApolloClasses")!!.outcome == TaskOutcome.SUCCESS)

    assert(File(dir, "build/generated/source/apollo/debug/0/com/example/type/CustomType.java")
        .readText()
        .contains("return Date.class;"))

    val text = File(dir, "build.gradle").readText()
    File(dir, "build.gradle").writeText(text.replace(apolloBlock, ""))
  }
}