plugins {
  kotlin("jvm")
  id("java-gradle-plugin")
  id("com.gradle.plugin-publish")
  id("com.gradleup.gr8")
}


// Configuration for extra jar to pass to R8 to give it more context about what can be relocated
configurations.create("gr8Classpath")
// Configuration dependencies that will be shadowed
val shadeConfiguration = configurations.create("shade")

dependencies {
  compileOnly(groovy.util.Eval.x(project, "x.dep.minGradleApi"))
  //compileOnly(groovy.util.Eval.x(project, "x.dep.gradleApi"))
  compileOnly(groovy.util.Eval.x(project, "x.dep.kotlinPluginMin"))
  compileOnly(groovy.util.Eval.x(project, "x.dep.android.minPlugin"))

  /**
   * OkHttp has some bytecode that checks for Conscrypt at runtime (https://github.com/square/okhttp/blob/71427d373bfd449f80178792fe231f60e4c972db/okhttp/src/main/kotlin/okhttp3/internal/platform/ConscryptPlatform.kt#L59)
   * Put this in the classpath so that R8 knows it can relocate DisabledHostnameVerifier as the superclass is not package-private
   *
   * Keep in sync with https://github.com/square/okhttp/blob/71427d373bfd449f80178792fe231f60e4c972db/buildSrc/src/main/kotlin/deps.kt#L24
   */
  add("gr8Classpath", "org.conscrypt:conscrypt-openjdk-uber:2.5.2")

  add("shade", "org.jetbrains.kotlin:kotlin-stdlib")
  add("shade", projects.apolloCompiler)
  add("shade", projects.apolloAst)

  add("shade", groovy.util.Eval.x(project, "x.dep.okHttp.okHttp"))
  add("shade", groovy.util.Eval.x(project, "x.dep.moshi.moshi").toString()) {
    because("Needed for manual Json construction in `SchemaDownloader`")
  }

  testImplementation(groovy.util.Eval.x(project, "x.dep.junit"))
  testImplementation(groovy.util.Eval.x(project, "x.dep.truth"))
  testImplementation(groovy.util.Eval.x(project, "x.dep.assertj"))
  testImplementation(groovy.util.Eval.x(project, "x.dep.okHttp.mockWebServer"))
}

if (true) {
  gr8 {
    val shadowedJar = create("shadow") {
      proguardFile("rules.pro")
      configuration("shade")
      classPathConfiguration("gr8Classpath")

      exclude(".*MANIFEST.MF")
      exclude("META-INF/versions/9/module-info\\.class")
      exclude("META-INF/kotlin-stdlib.*\\.kotlin_module")

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

tasks.withType<Test> {
  dependsOn(":apollo-annotations:publishAllPublicationsToPluginTestRepository")
  dependsOn(":apollo-api:publishAllPublicationsToPluginTestRepository")
  dependsOn(":apollo-ast:publishAllPublicationsToPluginTestRepository")
  dependsOn(":apollo-normalized-cache-api:publishAllPublicationsToPluginTestRepository")
  dependsOn(":apollo-mpp-utils:publishAllPublicationsToPluginTestRepository")
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
      displayName = "Apollo Kotlin GraphQL client plugin."
      description = "Automatically generates typesafe java and kotlin models from your GraphQL files."
      implementationClass = "com.apollographql.apollo3.gradle.internal.ApolloPlugin"
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
