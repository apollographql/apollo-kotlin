plugins {
  id("org.jetbrains.kotlin.jvm")
  id("java-gradle-plugin")
  id("com.gradle.plugin-publish")
  id("com.gradleup.gr8")
}

apolloLibrary(
    namespace = "com.apollographql.apollo.gradle.relocated",
    jvmTarget = 11 // AGP requires 11
)

// Configuration for extra jar to pass to R8 to give it more context about what can be relocated
configurations.create("gr8Classpath")
// Configuration dependencies that will be shadowed
val shadeConfiguration = configurations.create("shade")

// Set to false to skip relocation and save some building time during development
val relocateJar = System.getenv("APOLLO_RELOCATE_JAR")?.toBoolean() ?: true

dependencies {
  /**
   * OkHttp has some bytecode that checks for Conscrypt at runtime (https://github.com/square/okhttp/blob/71427d373bfd449f80178792fe231f60e4c972db/okhttp/src/main/kotlin/okhttp3/internal/platform/ConscryptPlatform.kt#L59)
   * Put this in the classpath so that R8 knows it can relocate DisabledHostnameVerifier as the superclass is not package-private
   *
   * Keep in sync with https://github.com/square/okhttp/blob/71427d373bfd449f80178792fe231f60e4c972db/buildSrc/src/main/kotlin/deps.kt#L24
   */
  add("gr8Classpath", "org.conscrypt:conscrypt-openjdk-uber:2.5.2")

  add("shade", project(":apollo-gradle-plugin-external"))

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
    val shadowedJar = create("shadow") {
      proguardFile("rules.pro")
      configuration("shade")
      classPathConfiguration("gr8Classpath")

      exclude(".*MANIFEST.MF")
      exclude("META-INF/versions/9/module-info\\.class")
      exclude("META-INF/kotlin-stdlib.*\\.kotlin_module")

      // Remove the following error:
      // /Users/mbonnin/.m2/repository/com/apollographql/apollo/apollo-gradle-plugin/3.3.3-SNAPSHOT/apollo-gradle-plugin-3.3.3-SNAPSHOT.jar!/META-INF/kotlinpoet.kotlin_module:
      // Module was compiled with an incompatible version of Kotlin. The binary version of its metadata is 1.7.1,
      // expected version is 1.5.1.
      exclude("META-INF/kotlinpoet.kotlin_module")

      //Remove the following error:
      // /Users/mbonnin/git/test-gradle-7-4/src/main/kotlin/Main.kt: (2, 5): Class 'kotlin.Unit' was compiled
      // with an incompatible version of Kotlin. The binary version of its metadata is 1.7.1, expected version
      // is 1.5.1.
      exclude("kotlin/Unit.class")

      // Remove proguard rules from dependencies, we'll manage them ourselves
      exclude("META-INF/proguard/.*")

      systemClassesToolchain {
        languageVersion.set(JavaLanguageVersion.of(8))
      }
    }

    // The java-gradle-plugin adds `gradleApi()` to the `api` implementation but it contains some JDK15 bytecode at
    // org/gradle/internal/impldep/META-INF/versions/15/org/bouncycastle/jcajce/provider/asymmetric/edec/SignatureSpi$EdDSA.class:
    // java.lang.IllegalArgumentException: Unsupported class file major version 59
    // So remove it
    val apiDependencies = project.configurations.getByName("api").dependencies
    apiDependencies.firstOrNull {
      it is FileCollectionDependency
    }.let {
      apiDependencies.remove(it)
    }

    configurations.named("compileOnly").configure {
      extendsFrom(shadeConfiguration)
    }
    configurations.named("testImplementation").configure {
      extendsFrom(shadeConfiguration)
    }

    replaceOutgoingJar2(shadowedJar)
  }
} else {
  configurations.named("implementation").configure {
    extendsFrom(shadeConfiguration)
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
      description = "Automatically generates typesafe java and kotlin models from your GraphQL files."
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

tasks.withType<Test> {
  dependsOn(":apollo-annotations:publishAllPublicationsToPluginTestRepository")
  dependsOn(":apollo-api:publishAllPublicationsToPluginTestRepository")
  dependsOn(":apollo-ast:publishAllPublicationsToPluginTestRepository")
  dependsOn(":apollo-normalized-cache-api:publishAllPublicationsToPluginTestRepository")
  dependsOn(":apollo-mpp-utils:publishAllPublicationsToPluginTestRepository")
  dependsOn(":apollo-compiler:publishAllPublicationsToPluginTestRepository")
  dependsOn(":apollo-gradle-plugin-external:publishAllPublicationsToPluginTestRepository")
  dependsOn(":apollo-tooling:publishAllPublicationsToPluginTestRepository")
  dependsOn("publishAllPublicationsToPluginTestRepository")

  dependsOn("cleanStaleTestProjects")

  addRelativeInput("testFiles", "testFiles")
  addRelativeInput("testProjects", "testProjects")

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
