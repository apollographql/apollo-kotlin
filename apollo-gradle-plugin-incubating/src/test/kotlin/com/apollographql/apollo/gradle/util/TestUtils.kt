package com.apollographql.apollo.gradle.util

import com.apollographql.apollo.compiler.child
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.hamcrest.CoreMatchers.containsString
import org.hamcrest.CoreMatchers.not
import org.junit.Assert.assertThat
import java.io.File

object TestUtils {
  class Plugin(val artifact: String?, val id: String)

  val javaPlugin = Plugin(id = "java", artifact = null)
  val androidApplicationPlugin = Plugin(id = "com.android.application", artifact = "android.plugin")
  val androidLibraryPlugin = Plugin(id = "com.android.library", artifact = "android.plugin")
  val kotlinJvmPlugin = Plugin(id = "org.jetbrains.kotlin.jvm", artifact = "kotlin.plugin")
  val kotlinAndroidPlugin = Plugin(id = "org.jetbrains.kotlin.android", artifact = "kotlin.plugin")
  val apolloPlugin = Plugin(id = "com.apollographql.apollo", artifact = "apollo.pluginIncubating")
  val apolloPluginAndroid = Plugin(id = "com.apollographql.android", artifact = "apollo.pluginIncubating")

  fun withProject(usesKotlinDsl: Boolean,
                  plugins: List<Plugin>,
                  apolloConfiguration: String,
                  isFlavored: Boolean = false,
                  block: (File) -> Unit) {
    val source = fixturesDirectory()
    val dest = File(System.getProperty("user.dir")).child("build", "testProject")

    dest.deleteRecursively()

    source.child("starwars").copyRecursively(target = dest.child("src", "main", "graphql", "com", "example"))
    source.child("gradle", "settings.gradle").copyTo(target = dest.child("settings.gradle"))

    val isAndroid = plugins.firstOrNull { it.id.startsWith("com.android") } != null
    val hasKotlin = plugins.firstOrNull { it.id.startsWith("org.jetbrains.kotlin") } != null

    if (usesKotlinDsl) {
      val applyLines = plugins.map { "apply(plugin = \"${it.id}\")" }.joinToString("\n")
      val classPathLines = plugins.filter { it.artifact != null }
          .map { "classpath(classpathDep(\"${it.artifact!!}\"))" }
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

      if (hasKotlin) {
        buildscript = buildscript.replace(
            "// ADD DEPENDENCIES HERE",
            "add(\"implementation\", kotlinDep(\"kotlin.stdlib\"))")
      }

      File(dest, "build.gradle.kts").writeText(buildscript)
    } else {
      val applyLines = plugins.map { "apply plugin: \"${it.id}\"" }.joinToString("\n")
      val classPathLines = plugins.filter { it.artifact != null }.map { "classpath(dep.${it.artifact})" }.joinToString("\n")

      var buildscript = File(source, "gradle/build.gradle.template")
          .readText()
          .replace("// ADD BUILDSCRIPT DEPENDENCIES HERE", classPathLines)
          .replace("// ADD PLUGINS HERE", applyLines)
          .replace("// ADD APOLLO CONFIGURATION HERE", apolloConfiguration)

      if (isAndroid) {
        var androidConfiguration = """
        |android {
        |  compileSdkVersion androidConfig.compileSdkVersion
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

      if (hasKotlin) {
        buildscript = buildscript.replace("// ADD DEPENDENCIES HERE", "implementation dep.kotlin.stdLib")
      }

      File(dest, "build.gradle").writeText(buildscript)
    }

    if (isAndroid) {
      source.child("manifest", "AndroidManifest.xml").copyTo(dest.child("src", "main", "AndroidManifest.xml"))
      File(dest, "local.properties").writeText("sdk.dir=${androidHome()}\n")
    }

    block(dest)

    dest.deleteRecursively()
  }

  /**
   * creates a simple java non-android non-kotlin-gradle project
   */
  fun withSimpleProject(apolloConfiguration: String = "", block: (File) -> Unit) = withProject(
      usesKotlinDsl = false,
      plugins = listOf(javaPlugin, apolloPlugin),
      apolloConfiguration = apolloConfiguration
  ) { dir ->
    fixturesDirectory().child("java").copyRecursively(dir.child("src", "main", "java"))
    block(dir)
  }

  fun executeGradle(projectDir: File, vararg args: String): BuildResult {
    return GradleRunner.create()
        .forwardStdOutput(System.out.writer())
        .forwardStdError(System.err.writer())
        .withProjectDir(projectDir)
        .withArguments("--stacktrace", *args)
        .build()
  }

  fun executeTask(task: String, projectDir: File, vararg args: String): BuildResult {
    return executeGradle(projectDir, task, *args)
  }

  fun assertFileContains(projectDir: File, path: String, content: String) {
    val text = projectDir.generatedChild(path).readText()
    assertThat(text, containsString(content))
  }

  fun assertFileDoesNotContain(projectDir: File, path: String, content: String) {
    val text = projectDir.generatedChild(path).readText()
    assertThat(text, not(containsString(content)))
  }

  fun fileContains(projectDir: File, path: String, content: String): Boolean {
    return projectDir.generatedChild(path).readText()
        .contains(content)
  }

  fun fixturesDirectory() = File(System.getProperty("user.dir")).child("src", "test", "files")
}

fun File.generatedChild(path: String) = child("build", "generated", "source", "apollo", path)

fun File.replaceInText(oldValue: String, newValue: String) {
  val text = readText()
  writeText(text.replace(oldValue, newValue))
}