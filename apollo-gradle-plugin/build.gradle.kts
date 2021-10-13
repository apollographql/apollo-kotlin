plugins {
  id("java")
  id("org.jetbrains.kotlin.jvm")
  id("java-gradle-plugin")
  id("com.gradle.plugin-publish")
  id("com.github.johnrengelman.shadow")
  kotlin("kapt")
}

// groovy strings with double quotes are GString.
// groovy strings with single quotes are java.lang.String
// In all cases, gradle APIs take Any so just feed them whatever is returned
fun dep(key: String) = (extra["dep"] as Map<*, *>)[key]!!

fun Any.dot(key: String): Any {
  return (this as Map<String, *>)[key]!!
}

metalava {
  hiddenPackages += setOf("com.apollographql.apollo.gradle.internal")
}

/**
 * Special configuration to be included in resulting shadowed jar, but not added to the generated pom and gradle
 * metadata files.
 * Largely inspired by Ktlint https://github.com/JLLeitschuh/ktlint-gradle/blob/530aa9829abea01e4c91e8798fb7341c438aac3b/plugin/build.gradle.kts
 */
val shadowImplementation by configurations.creating
configurations["compileOnly"].extendsFrom(shadowImplementation)
configurations["testImplementation"].extendsFrom(shadowImplementation)

fun addShadowImplementation(dependency: Dependency) {
  dependency as ModuleDependency
  /**
   * Exclude the kotlin-stdlib from the fatjar because:
   * 1. it's less bytes to download and load as the stdlib should be on the classpath already
   * 2. there's a weird bug where trying to relocate the stdlib will also rename the "kotlin"
   * strings inside the plugin so something like `extensions.findByName("kotlin")` becomes
   * `extensions.findByName("com.apollographql.relocated.kotlin")
   * See https://github.com/johnrengelman/shadow/issues/232 for more details
   */
  dependency.exclude(group = "org.jetbrains.kotlin", module = "kotlin-stdlib")
  dependency.exclude(group = "org.jetbrains.kotlin", module = "kotlin-stdlib-jdk7")
  dependency.exclude(group = "org.jetbrains.kotlin", module = "kotlin-stdlib-jdk8")
  dependency.exclude(group = "org.jetbrains.kotlin", module = "kotlin-stdlib-common")
  dependency.exclude(group = "org.jetbrains.kotlin", module = "kotlin-reflect")

  dependencies.add("shadowImplementation", dependency)
}
dependencies {
  compileOnly(gradleApi())
  compileOnly(groovy.util.Eval.x(project, "x.dep.kotlin.plugin"))
  compileOnly(groovy.util.Eval.x(project, "x.dep.android.minPlugin"))

  addShadowImplementation(project(":apollo-compiler"))
  addShadowImplementation(project(":apollo-api"))

  addShadowImplementation(create(groovy.util.Eval.x(project, "x.dep.okHttp.okHttp4")))
  // Needed for manual Json construction in `SchemaDownloader`
  addShadowImplementation(create(groovy.util.Eval.x(project, "x.dep.moshi.moshi")))
  addShadowImplementation(create(groovy.util.Eval.x(project, "x.dep.graphqlJava")))

  testImplementation(groovy.util.Eval.x(project, "x.dep.junit"))
  testImplementation(groovy.util.Eval.x(project, "x.dep.truth"))
  testImplementation(groovy.util.Eval.x(project, "x.dep.okHttp.mockWebServer4"))
}

val shadowJarTask = tasks.named("shadowJar", com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar::class.java)
val shadowPrefix = "com.apollographql.relocated"

shadowJarTask.configure {
  configurations = listOf(shadowImplementation)

  /**
   * This list is built by unzipping the fatjar and looking at .class files inside with something like:
   *
   * val dir  = args[0]
   * val classPaths = File(dir).walk().filter { it.isFile }.map {
   *   it.parentFile.relativeTo(File(dir)).path.replace("/", ".")
   * }.distinct().sorted()
   *
   * Things to consider:
   * - We do not ship kotlin-stdlib in the fat jar as it should be provided by Gradle already (see [addShadowImplementation])
   * - I'm hoping kotlin-reflect is also provided by Gradle. In the tests I've made, it looks like it is and relocating it fails
   * with java.lang.NoSuchMethodError: 'com.apollographql.relocated.kotlin.reflect.KClass kotlin.jvm.internal.Reflection.getOrCreateKotlinClass(java.lang.Class)
   * - We do not relocate "com.apollographql.apollo.*" as the codegen has a lot of hardcoded strings inside that shouldn't be relocated
   * as they are used at runtime
   * - If we relocate a deep dependency (such as okio), we must relocate all intermediate dependencies (such as moshi/okhttp) or else
   * this will clash with any other version in the classpath
   * - When there are multiple subpackages, we usually include a shared prefx package to avoid this list being huge... But we don't
   * want these packages to be too short either as it increases the risk of a string being renamed
   */
  listOf(
      "com.benasher44.uuid",
      "com.squareup.kotlinpoet",
      "com.squareup.javapoet",
      "com.squareup.moshi",
      "okhttp3",
      "okio",
      "org.antlr"
  ).forEach { packageName ->
    relocate(packageName, "com.apollographql.relocated.$packageName")
  }
}

tasks.getByName("jar").enabled = false
tasks.getByName("jar").dependsOn(shadowJarTask)

configurations {
  configureEach {
    outgoing {
      val removed = artifacts.removeIf { it.classifier.isNullOrEmpty() }
      if (removed) {
        artifact(tasks.shadowJar) {
          // Pom and maven consumers do not like the `-all` default classifier
          classifier = ""
        }
      }
    }
  }
  // used by plugin-publish plugin
  archives {
    extendsFrom(signatures.get())
  }
}

tasks.withType<Test> {
  dependsOn(":apollo-api:publishAllPublicationsToPluginTestRepository")
  dependsOn(":apollo-compiler:publishAllPublicationsToPluginTestRepository")
  dependsOn("publishAllPublicationsToPluginTestRepository")

  inputs.dir("src/test/files")
  inputs.dir("testProjects")
}

pluginBundle {
  website = "https://github.com/apollographql/apollo-android"
  vcsUrl = "https://github.com/apollographql/apollo-android"
  tags = listOf("graphql", "apollo", "apollographql", "kotlin", "java", "jvm", "android", "graphql-client")
}

gradlePlugin {
  plugins {
    create("apolloGradlePlugin") {
      id = "com.apollographql.apollo"
      displayName = "Apollo Android GraphQL client plugin."
      description = "Automatically generates typesafe java and kotlin models from your GraphQL files."
      implementationClass = "com.apollographql.apollo.gradle.internal.ApolloPlugin"
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
