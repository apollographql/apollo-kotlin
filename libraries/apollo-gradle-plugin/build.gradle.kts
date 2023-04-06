plugins {
  id("org.jetbrains.kotlin.jvm")
  id("java-gradle-plugin")
  id("com.gradle.plugin-publish")
  id("apollo.library")
  id("com.gradleup.gr8")
}

apolloLibrary {
  // See GradleVersionTests.kt the Gradle runner will choke on more recent versions
  // Keep in sync with TestUtils.kt
  runTestsWithJavaVersion(11)
}

// Configuration for extra jar to pass to R8 to give it more context about what can be relocated
configurations.create("gr8Classpath")
// Configuration dependencies that will be shadowed
val shadeConfiguration = configurations.create("shade")

// Set to false to skip relocation and save some building time during development
val relocateJar = true

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
  testImplementation(golatac.lib("junit"))
  testImplementation(golatac.lib("truth"))
  testImplementation(golatac.lib("assertj"))
  testImplementation(golatac.lib("okhttp.mockwebserver"))
  testImplementation(golatac.lib("okhttp.tls"))
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
      // /Users/mbonnin/.m2/repository/com/apollographql/apollo3/apollo-gradle-plugin/3.3.3-SNAPSHOT/apollo-gradle-plugin-3.3.3-SNAPSHOT.jar!/META-INF/kotlinpoet.kotlin_module:
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
    }
    removeGradleApiFromApi()

    configurations.named("compileOnly").configure {
      extendsFrom(shadeConfiguration)
    }
    configurations.named("testImplementation").configure {
      extendsFrom(shadeConfiguration)
    }
    replaceOutgoingJar(shadowedJar)
  }
} else {
  configurations.named("implementation").configure {
    extendsFrom(shadeConfiguration)
  }
}


gradlePlugin {
  website.set("https://github.com/apollographql/apollo-kotlin")
  vcsUrl.set("https://github.com/apollographql/apollo-kotlin")

  plugins {
    create("apolloGradlePlugin") {
      id = "com.apollographql.apollo3"
      displayName = "Apollo Kotlin GraphQL client plugin."
      description = "Automatically generates typesafe java and kotlin models from your GraphQL files."
      implementationClass = "com.apollographql.apollo3.gradle.internal.ApolloPlugin"
      tags.set(listOf("graphql", "apollo", "plugin"))
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

  inputs.dir("src/test/files")
  inputs.dir("testProjects")

  maxParallelForks = (Runtime.getRuntime().availableProcessors() / 2).takeIf { it > 0 } ?: 1

  doFirst {
    /**
     * Remove stale testProject directories
     */
    buildDir.listFiles().forEach {
      if (it.isDirectory && it.name.startsWith("testProject")) {
        it.deleteRecursively()
      }
    }
  }
}

