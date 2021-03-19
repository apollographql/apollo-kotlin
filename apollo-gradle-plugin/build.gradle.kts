plugins {
  kotlin("jvm")
  id("java-gradle-plugin")
  id("com.gradle.plugin-publish")
  id("com.github.johnrengelman.shadow")
}


metalava {
  hiddenPackages += setOf("com.apollographql.apollo3.gradle.internal")
}

/**
 * Special configuration to be included in resulting shadowed jar, but not added to the generated pom and gradle
 * metadata files.
 * Largely inspired by Ktlint https://github.com/JLLeitschuh/ktlint-gradle/blob/530aa9829abea01e4c91e8798fb7341c438aac3b/plugin/build.gradle.kts
 */
val shadowImplementation by configurations.creating
configurations["compileOnly"].extendsFrom(shadowImplementation)
configurations["testImplementation"].extendsFrom(shadowImplementation)

dependencies {
  compileOnly(gradleApi())
  compileOnly(groovy.util.Eval.x(project, "x.dep.kotlin.plugin"))
  compileOnly(groovy.util.Eval.x(project, "x.dep.android.minPlugin"))
  // kotlin-reflect is transitively pulled by the android plugin, make it explicit so that it uses the same version as the rest of kotlin libs
  compileOnly(groovy.util.Eval.x(project, "x.dep.kotlin.reflect"))

  shadowImplementation(project(":apollo-compiler"))
  shadowImplementation(project(":apollo-api"))
  shadowImplementation(groovy.util.Eval.x(project, "x.dep.okHttp.okHttp4").toString())
  // Needed for manual Json construction in `SchemaDownloader`
  shadowImplementation(groovy.util.Eval.x(project, "x.dep.moshi.moshi").toString())

  testImplementation(groovy.util.Eval.x(project, "x.dep.junit"))
  testImplementation(groovy.util.Eval.x(project, "x.dep.truth"))
  testImplementation(groovy.util.Eval.x(project, "x.dep.okHttp.mockWebServer4"))
}

val shadowJarTask = tasks.named("shadowJar", com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar::class.java)
val shadowPrefix = "com.apollographql.relocated"

shadowJarTask.configure {
  configurations = listOf(shadowImplementation)
  relocate("org.antlr", "com.apollographql.relocated.org.antlr")
  relocate("okio", "com.apollographql.relocated.okio")
  relocate("okhttp3", "com.apollographql.relocated.okhttp3")
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
      id = "com.apollographql.apollo3"
      displayName = "Apollo Android GraphQL client plugin."
      description = "Automatically generates typesafe java and kotlin models from your GraphQL files."
      implementationClass = "com.apollographql.apollo3.gradle.internal.ApolloPlugin"
    }
  }
}

tasks.withType(org.jetbrains.kotlin.gradle.tasks.KotlinCompile::class.java) {
  kotlinOptions {
    // Gradle forces 1.3.72 for the time being so compile against 1.3 stdlib for the time being
    // See https://issuetracker.google.com/issues/166582569
    apiVersion = "1.3"
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
