package util


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

  val kotlinJvmPlugin = Plugin(id = "org.jetbrains.kotlin.jvm", artifact = "libs.plugins.kotlin.jvm")
  val apolloPlugin = Plugin(id = "com.apollographql.apollo", artifact = "libs.plugins.apollo")

  fun <T> withDirectory(testDir: String? = null, block: (File) -> T): T {
    val dest = if (testDir == null) {
      // Tests are run in parallel, make sure 2 tests do not clobber themselves
      File.createTempFile("testProject", "", File(System.getProperty("user.dir")).resolve("build"))
    } else {
      File(System.getProperty("user.dir")).resolve("build/$testDir")
    }
    dest.deleteRecursively()

    // See https://github.com/apollographql/apollo-kotlin/issues/2184
    dest.mkdirs()
    File(dest, "gradle.properties").writeText("""
      |org.gradle.jvmargs=-Xmx4g 
      |
      |org.gradle.unsafe.isolated-projects=true
    """.trimMargin()
    )

    // dest is kept around for debug purposes. All test directories are removed
    // with the `cleanStaleTestProject` tasks before the next run
    return block(dest)
  }

  fun withProject(
      usesKotlinDsl: Boolean,
      plugins: List<Plugin>,
      apolloConfiguration: String,
      isFlavored: Boolean = false,
      graphqlPath: String = "starwars",
      block: (File) -> Unit,
  ) = withDirectory {
    val source = fixturesDirectory()
    val dest = it

    File(source, graphqlPath).copyRecursively(target = File(dest, "src/main/graphql/com/example"))
    File(source, "gradle/settings.gradle").copyTo(target = File(dest, "settings.gradle"))

    val isAndroid = plugins.firstOrNull { it.id.startsWith("com.android") } != null

    val buildscript = buildString {
      appendLine("plugins {")
      plugins.forEach {
        appendLine("  alias(${it.artifact})")
      }
      appendLine("}")
      appendLine()

      appendLine()
      append("""
        dependencies {
          add("implementation", libs.apollo.api)
        }
        
        java.toolchain {
          languageVersion.set(JavaLanguageVersion.of(11))
        }
      """.trimIndent()
      )

      appendLine()

      append(apolloConfiguration)

      appendLine()

      if (isAndroid) {
        append("""
            |android {
            |  compileSdkVersion(libs.versions.android.sdkversion.compile.get().toInteger())
            |  namespace = "com.example"
            |  
            |  // Keep in sync with apollo-gradle-plugin/build.gradle.kts
            |  // https://issuetracker.google.com/issues/260059413  
            |  compileOptions {
            |    sourceCompatibility = 11
            |    targetCompatibility = 11
            |  }
            |
          """.trimMargin()
        )

        if (isFlavored) {
          append("""
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
          )
        }

        append("""
            |}
          """.trimMargin()
        )
      }
    }

    val filename = if (usesKotlinDsl) "build.gradle.kts" else "build.gradle"
    File(dest, filename).writeText(buildscript)

    if (isAndroid) {
      File(source, "manifest/AndroidManifest.xml").copyTo(File(dest, "src/main/AndroidManifest.xml"))
      File(dest, "local.properties").writeText("sdk.dir=${androidHome()}\n")
    }

    block(dest)
  }

  fun withGeneratedAccessorsProject(apolloConfiguration: String, block: (File) -> Unit) = withProject(
      usesKotlinDsl = true,
      plugins = listOf(kotlinJvmPlugin, apolloPlugin),
      apolloConfiguration = apolloConfiguration,
      block = block
  )

  fun <T> withTestProject(name: String, testDir: String? = null, block: (File) -> T): T = withDirectory(testDir) { dir ->
    File(System.getProperty("user.dir"), "testProjects/$name").copyRecursively(dir, overwrite = true)

    block(dir)
  }

  /**
   * creates a simple java non-android non-kotlin-gradle project
   */
  fun withSimpleProject(
      apolloConfiguration: String = """
    apollo {
      service("service") {
        packageNamesFromFilePaths()
      }
    }
  """.trimIndent(),
      block: (File) -> Unit,
  ) = withProject(
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
    val debug = false

    return GradleRunner.create()
        .apply {
          if (debug) {
            forwardStdOutput(System.out.writer())
            forwardStdError(System.err.writer())
          } else {
            val bh = blackholeSink().buffer().outputStream().writer()
            forwardStdOutput(bh)
            forwardStdError(bh)
          }
          if (gradleVersion != null) {
            withGradleVersion(gradleVersion)
          }
        }
        .withProjectDir(projectDir)
        /*
         * Disable withDebug() because it breaks with CC
         * See https://github.com/gradle/gradle/issues/22765#issuecomment-1339427241
         */
        //.withDebug(true)
        .withArguments(*args)
        .build()
  }

  fun executeTask(task: String, projectDir: File, vararg args: String): BuildResult {
    return executeGradleWithVersion(projectDir, null, task, *args)
  }

  fun assertFileContains(projectDir: File, path: String, content: String) {
    val text = projectDir.generatedSource(path).readText()
    Truth.assertThat(text).contains(content)
  }

  fun fixturesDirectory() = File(System.getProperty("user.dir"), "testFiles")

  fun executeTaskAndAssertSuccess(task: String, dir: File) {
    val result = executeTask(task, dir)
    Assert.assertEquals(TaskOutcome.SUCCESS, result.task(task)?.outcome)
  }

  fun setVersionsUnderTest(dir: File, versionsUnderTest: VersionsUnderTest?) {
    if (versionsUnderTest == null) {
      return
    }
    dir.resolve("build.gradle.kts").apply {
      writeText(
          readText()
              .replace("alias(libs.plugins.android.library)", "id(\"com.android.library\").version(\"${versionsUnderTest.agp}\")")
              .replace("alias(libs.plugins.android.application)", "id(\"com.android.application\").version(\"${versionsUnderTest.agp}\")")
              .replace("alias(libs.plugins.android.kmp.library)", "id(\"com.android.kotlin.multiplatform.library\").version(\"${versionsUnderTest.agp}\")")
              .let {
                // poor's man semver parsing because I don't want to add another dependency just for semver
                if (versionsUnderTest.agp.startsWith("9")) {
                  // AGP 9 wraps KGP, applying it is an error
                  it.replace("alias(libs.plugins.kotlin.android)", "")
                      .replace("alias(libs.plugins.kotlin.jvm)", "")
                } else {
                  it.replace("alias(libs.plugins.kotlin.android)", "id(\"org.jetbrains.kotlin.android\").version(\"${versionsUnderTest.kgp}\")")
                      .replace("alias(libs.plugins.kotlin.jvm)", "id(\"org.jetbrains.kotlin.jvm\").version(\"${versionsUnderTest.kgp}\")")
                }
              }
              .replace("alias(libs.plugins.kotlin.multiplatform)", "id(\"org.jetbrains.kotlin.multiplatform\").version(\"${versionsUnderTest.kgp}\")")
              .replace("compileSdk = libs.versions.android.sdkversion.compile.get().toInt()", "compileSdk = ${versionsUnderTest.compileSdk}")
      )
    }
    if (!versionsUnderTest.isolatedProjects) {
      dir.disableIsolatedProjects()
    }
  }

}

/**
 * A few versions to test with.
 *
 * We just can't test across all possible variations of different tools, especially
 * because some of them have dependencies.
 */
val agp8_kgp2_0 = VersionsUnderTest(
    agp = "8.0.0",
    kgp = "2.0.0",
    compileSdk = 33,
    gradle = "8.0",
    isolatedProjects = false
)
val agp8_13_0_versions = VersionsUnderTest(
    agp = "8.13.0",
    kgp = "2.1.0",
)
val agp8_13_kgp_2_2_20 = VersionsUnderTest(
    agp = "8.13.0",
    kgp = "2.2.20",
)
val agp9_versions = VersionsUnderTest(
    agp = "9.0.0-alpha05",
    kgp = "2.2.20",
)

class VersionsUnderTest(
    val agp: String,
    val kgp: String,
    val compileSdk: Int = 36,
    val gradle: String = "9.0.0",
    val isolatedProjects: Boolean = true,
)

fun File.generatedSource(path: String, serviceName: String = "service") =
  File(this, "build/generated/source/apollo/${serviceName}").resolve(path)

fun File.replaceInText(oldValue: String, newValue: String) {
  val text = readText()
  writeText(text.replace(oldValue, newValue))
}

fun File.disableIsolatedProjects() {
  resolve("gradle.properties").let {
    it.writeText(it.readText().replace("org.gradle.unsafe.isolated-projects=true", ""))
  }
}