import org.jetbrains.kotlin.gradle.dsl.KotlinVersion

plugins {
  id("org.jetbrains.kotlin.jvm")
  id("java-gradle-plugin")
  id("com.gradle.plugin-publish")
  id("com.gradleup.gr8")
}

val jvmTarget = 11 // AGP requires 11
apolloLibrary(
    namespace = "com.apollographql.apollo.gradle.relocated",
    jvmTarget = jvmTarget,
    kotlinCompilerOptions = KotlinCompilerOptions(KotlinVersion.KOTLIN_1_9)
)

// Set to false to skip relocation and save some building time during development
val relocateJar = System.getenv("APOLLO_RELOCATE_JAR")?.toBoolean() ?: true

val shadowedDependencies = configurations.create("shadowedDependencies")

dependencies {
  add(shadowedDependencies.name, project(path = ":apollo-gradle-plugin-external", configuration = "runtimeElements"))

  testImplementation(project(":apollo-ast"))
  testImplementation(libs.junit)
  testImplementation(libs.truth)
  testImplementation(libs.assertj)
  testImplementation(libs.okhttp.mockwebserver)
  testImplementation(libs.okhttp.tls)

  testImplementation(libs.apollo.execution)
  testImplementation(libs.apollo.execution.http4k)

  testImplementation(platform(libs.http4k.bom.get()))
  testImplementation(libs.http4k.core)
  testImplementation(libs.http4k.server.jetty)
  testImplementation(libs.slf4j.nop.get().toString()) {
    because("jetty uses SL4F")
  }
}


if (relocateJar) {
  gr8 {
    val shadowedJar = create("default") {
      addProgramJarsFrom(shadowedDependencies)
      addProgramJarsFrom(tasks.getByName("jar"))
      // 97951e94471814d3148bfb1ff0a4617f1781a1fe
      r8Version("31d8f0676a22526f2ce7fb39cfdd83671536eb3c")
      systemClassesToolchain {
        languageVersion.set(JavaLanguageVersion.of(jvmTarget))
      }
      proguardFile("rules.pro")
      registerFilterTransform(listOf(".*/impldep/META-INF/versions/.*"))
    }

    removeGradleApiFromApi()
    configurations.named("compileOnly").configure {
      extendsFrom(shadowedDependencies)
    }
    configurations.named("testImplementation").configure {
      extendsFrom(shadowedDependencies)
    }

    replaceOutgoingJar2(shadowedJar)
  }
} else {
  configurations.named("implementation").configure {
    extendsFrom(shadowedDependencies)
  }
}

fun replaceOutgoingJar2(newJar: Any) {
  project.configurations.configureEach {
    outgoing {
      val removed = artifacts.removeIf {
        it.name == "apollo-gradle-plugin" && it.type == "jar" && it.classifier.isNullOrEmpty()
      }
      if (removed) {
        artifact(newJar) {
          // Pom and maven consumers do not like the `-all` or `-shadowed` classifiers
          classifier = ""
        }
      }
    }
  }
}

gradlePlugin {
  website.set("https://github.com/apollographql/apollo-kotlin")
  vcsUrl.set("https://github.com/apollographql/apollo-kotlin")

  plugins {
    create("apolloGradlePlugin") {
      id = "com.apollographql.apollo"
      displayName = "Apollo Kotlin GraphQL client plugin."
      description = """
        ⚠️ Newer versions of the Apollo Kotlin Gradle Plugin (v5+) are only published on Maven Central.
         
        See https://github.com/gradle/plugin-portal-requests/issues/225 for more details and https://www.apollographql.com/docs/kotlin/advanced/plugin-configuration for documentation about the Gradle plugin.
        """.trimIndent()
      implementationClass = "com.apollographql.apollo.gradle.internal.ApolloPlugin"
      tags.set(listOf("graphql", "apollo"))
    }
  }
}

/**
 * This is so that the plugin marker pom contains a <scm> tag
 * It was recommended by the Gradle support team.
 */
configure<PublishingExtension> {
  publications.configureEach {
    if (name == "apolloGradlePluginPluginMarkerMaven") {
      this as MavenPublication
      pom {
        scm {
          url.set(findProperty("POM_SCM_URL") as String?)
          connection.set(findProperty("POM_SCM_CONNECTION") as String?)
          developerConnection.set(findProperty("POM_SCM_DEV_CONNECTION") as String?)
        }
      }
    }
  }
}

tasks.register("cleanStaleTestProjects") {
  /**
   * Remove stale testProject directories
   */
  val buildFiles = layout.buildDirectory.asFile.get().listFiles()
  doFirst {
    buildFiles?.forEach {
      if (it.isDirectory && it.name.startsWith("testProject")) {
        it.deleteRecursively()
      }
    }
  }
}

tasks.register("publishDependencies") {
  dependsOn("publishAllPublicationsToPluginTestRepository")
  dependsOn(":apollo-annotations:publishAllPublicationsToPluginTestRepository")
  dependsOn(":apollo-api:publishAllPublicationsToPluginTestRepository")
  dependsOn(":apollo-ast:publishAllPublicationsToPluginTestRepository")
  dependsOn(":apollo-normalized-cache-api:publishAllPublicationsToPluginTestRepository")
  dependsOn(":apollo-mpp-utils:publishAllPublicationsToPluginTestRepository")
  dependsOn(":apollo-compiler:publishAllPublicationsToPluginTestRepository")
  dependsOn(":apollo-gradle-plugin-external:publishAllPublicationsToPluginTestRepository")
  dependsOn(":apollo-tooling:publishAllPublicationsToPluginTestRepository")
}

tasks.withType<Test> {
  dependsOn("publishDependencies")
  dependsOn("cleanStaleTestProjects")

  addRelativeInput("testFiles", "testFiles")
  addRelativeInput("testProjects", "testProjects")

  maxHeapSize = "1g"

  maxParallelForks = (Runtime.getRuntime().availableProcessors() / 2).takeIf { it > 0 } ?: 1
}

val allTests = tasks.create("allTests")
tasks.check {
  dependsOn(allTests)
}

fun createTests(javaVersion: Int) {
  val sourceSet = sourceSets.create("test-java$javaVersion")

  configurations[sourceSet.implementationConfigurationName].extendsFrom(configurations.testImplementation.get())
  dependencies.add(sourceSet.implementationConfigurationName, sourceSets.getByName("test").output)
  configurations[sourceSet.runtimeOnlyConfigurationName].extendsFrom(configurations.testRuntimeOnly.get())

  val task = tasks.register<Test>("testJava$javaVersion") {
    description = "Runs integration tests for Java $javaVersion."
    group = "verification"
    useJUnit()

    testClassesDirs = sourceSet.output.classesDirs
    classpath = configurations[sourceSet.runtimeClasspathConfigurationName] + sourceSet.output

    environment("APOLLO_RELOCATE_JAR", System.getenv("APOLLO_RELOCATE_JAR"))
    setTestToolchain(project, this, javaVersion)
  }

  allTests.dependsOn(task)
}

tasks.named("test").configure {
  // Disable the default tests, they are empty
  enabled = false
}

listOf(11, 17).forEach { javaVersion ->
  createTests(javaVersion)
}

tasks.register("acceptAndroidLicenses") {
  val source = rootProject.file("android-licenses/android-sdk-preview-license")
  val target = rootProject.file("${System.getenv("ANDROID_HOME")}/licenses/android-sdk-preview-license")
  doLast {
    source.copyTo(target, overwrite = true)
  }
}

tasks.named("testJava17").configure {
  dependsOn("acceptAndroidLicenses")
}
