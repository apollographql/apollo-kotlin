package com.apollographql.apollo3.gradle.util


import com.google.common.truth.Truth
import okio.blackholeSink
import okio.buffer
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.Assert
import java.io.File

object TestUtils {
  class Plugin(val artifact: String?, val id: String)

  val androidApplicationPlugin = Plugin(id = "com.android.application", artifact = "android.plugin")
  val androidLibraryPlugin = Plugin(id = "com.android.library", artifact = "android.plugin")
  val kotlinJvmPlugin = Plugin(id = "org.jetbrains.kotlin.jvm", artifact = "kotlin.plugin")
  val kotlinAndroidPlugin = Plugin(id = "org.jetbrains.kotlin.android", artifact = "kotlin.plugin")
  val apolloPlugin = Plugin(id = "com.apollographql.apollo3", artifact = "apollo.plugin")

  fun withDirectory(block: (File) -> Unit) {
    val dest = File(File(System.getProperty("user.dir")), "build/testProject")
    dest.deleteRecursively()

    // See https://github.com/apollographql/apollo-android/issues/2184
    dest.mkdirs()
    File(dest, "gradle.properties").writeText("""
      |org.gradle.jvmargs=-Xmx4g 
      |
    """.trimMargin())

    block(dest)

    // It's ok to not delete the directory as it will be deleted before next test
    // During development, it's easy to keep the testProject around to investigate if something goes wrong
    // dest.deleteRecursively()
  }

  fun withProject(usesKotlinDsl: Boolean,
                  plugins: List<Plugin>,
                  apolloConfiguration: String,
                  isFlavored: Boolean = false,
                  graphqlPath: String = "starwars",
                  block: (File) -> Unit) = withDirectory {
    val source = fixturesDirectory()
    val dest = it

    File(source, graphqlPath).copyRecursively(target = File(dest, "src/main/graphql/com/example"))
    File(source, "gradle/settings.gradle").copyTo(target = File(dest, "settings.gradle"))

    val isAndroid = plugins.firstOrNull { it.id.startsWith("com.android") } != null

    if (usesKotlinDsl) {
      val applyLines = plugins.map { "apply(plugin = \"${it.id}\")" }.joinToString("\n")
      val classPathLines = plugins.filter { it.artifact != null }
          .map { "classpath(libs.${it.artifact!!})" }
          .joinToString("\n")

      var buildscript = File(source, "gradle/build.gradle.kts.template")
          .readText()
          .replace("// ADD BUILDSCRIPT DEPENDENCIES HERE", classPathLines)
          .replace("// ADD PLUGINS HERE", applyLines)
          .replace("// ADD APOLLO CONFIGURATION HERE", apolloConfiguration)

      if (isAndroid) {
        val androidConfiguration = """
        android {
          setCompileSdkVersion((extra["androidConfig"] as Map<String,*>).get("compileSdkVersion") as Int)
        }
      """.trimIndent()
        if (isFlavored) {
          throw IllegalArgumentException("flavored build using build.gradle.kts are not supported")
        }
        buildscript = buildscript.replace("// ADD ANDROID CONFIGURATION HERE", androidConfiguration)
      }

      File(dest, "build.gradle.kts").writeText(buildscript)
    } else {
      val applyLines = plugins.map { "apply plugin: \"${it.id}\"" }.joinToString("\n")
      val classPathLines = plugins.filter { it.artifact != null }.map { "classpath(libs.${it.artifact})" }.joinToString("\n")

      var buildscript = File(source, "gradle/build.gradle.template")
          .readText()
          .replace("// ADD BUILDSCRIPT DEPENDENCIES HERE", classPathLines)
          .replace("// ADD PLUGINS HERE", applyLines)
          .replace("// ADD APOLLO CONFIGURATION HERE", apolloConfiguration)

      if (isAndroid) {
        var androidConfiguration = """
        |android {
        |  compileSdkVersion libs.versions.android.sdkversion.compile.get().toInteger()
        |
      """.trimMargin()

        if (isFlavored) {
          androidConfiguration += """
          |  flavorDimensions "price"
          |  productFlavors {
          |    free {
          |      dimension 'price'
          |    }
          |
          |    paid {
          |      dimension 'price'
          |    }
          |  }
          |
          """.trimMargin()
        }

        androidConfiguration += """
        |}
      """.trimMargin()

        buildscript = buildscript.replace("// ADD ANDROID CONFIGURATION HERE", androidConfiguration)
      }

      File(dest, "build.gradle").writeText(buildscript)
    }

    if (isAndroid) {
      File(source, "manifest/AndroidManifest.xml").copyTo(File(dest, "src/main/AndroidManifest.xml"))
      File(dest, "local.properties").writeText("sdk.dir=${androidHome()}\n")
    }

    block(dest)
  }

  fun withGeneratedAccessorsProject(apolloConfiguration: String, block: (File) -> Unit) = withDirectory { dir ->
    File(fixturesDirectory(), "gradle/settings.gradle.kts").copyTo(File(dir, "settings.gradle.kts"))
    File(fixturesDirectory(), "gradle/build.gradle.kts").copyTo(File(dir, "build.gradle.kts"))

    File(dir, "build.gradle.kts").appendText(apolloConfiguration)

    block(dir)
  }

  fun withTestProject(name: String, block: (File) -> Unit) = withDirectory { dir ->
    File(System.getProperty("user.dir"), "testProjects/$name").copyRecursively(dir, overwrite = true)
    block(dir)
  }

  /**
   * creates a simple java non-android non-kotlin-gradle project
   */
  fun withSimpleProject(apolloConfiguration: String = """
    apollo {
      packageNamesFromFilePaths()
    }
  """.trimIndent(), block: (File) -> Unit) = withProject(
      usesKotlinDsl = false,
      plugins = listOf(kotlinJvmPlugin, apolloPlugin),
      apolloConfiguration = apolloConfiguration
  ) { dir ->
    File(fixturesDirectory(), "java").copyRecursively(File(dir, "src/main/java"))
    block(dir)
  }

  fun executeGradle(projectDir: File, vararg args: String): BuildResult {
    return executeGradleWithVersion(projectDir, null, *args)
  }

  fun executeGradleWithVersion(projectDir: File, gradleVersion: String?, vararg args: String): BuildResult {
    val output = blackholeSink().buffer()
    val error = blackholeSink().buffer()

    return GradleRunner.create()
        .forwardStdOutput(output.outputStream().writer())
        .forwardStdError(error.outputStream().writer())
        .withProjectDir(projectDir)
        .withDebug(true)
        .withArguments("--stacktrace", *args)
        .apply {
          if (gradleVersion != null) {
            withGradleVersion(gradleVersion)
          }
        }
        .build()
  }

  fun executeTask(task: String, projectDir: File, vararg args: String): BuildResult {
    return executeGradleWithVersion(projectDir, null, task, *args)
  }

  fun assertFileContains(projectDir: File, path: String, content: String) {
    val text = projectDir.generatedChild(path).readText()
    Truth.assertThat(text).contains(content)
  }

  fun fixturesDirectory() = File(System.getProperty("user.dir"), "src/test/files")

  fun executeTaskAndAssertSuccess(task: String, dir: File) {
    val result = executeTask(task, dir)
    Assert.assertEquals(TaskOutcome.SUCCESS, result.task(task)?.outcome)
  }
}

fun File.generatedChild(path: String) = File(this, "build/generated/source/apollo/$path")

fun File.replaceInText(oldValue: String, newValue: String) {
  val text = readText()
  writeText(text.replace(oldValue, newValue))
}
