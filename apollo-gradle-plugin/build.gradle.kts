plugins {
  id("java")
  kotlin("jvm")
  id("java-gradle-plugin")
  id("com.gradle.plugin-publish")
  kotlin("kapt")
}


dependencies {
  compileOnly(gradleApi())
  compileOnly(groovy.util.Eval.x(project, "x.dep.kotlin.plugin"))
  compileOnly(groovy.util.Eval.x(project, "x.dep.android.minPlugin"))
  // kotlin-reflect is transitively pulled by the android plugin, make it explicit so that it uses the same version as the rest of kotlin libs
  compileOnly(groovy.util.Eval.x(project, "x.dep.kotlin.reflect"))

  api(project(":apollo-compiler"))
  implementation(project(":apollo-api")) // for QueryDocumentMinifier
  implementation(groovy.util.Eval.x(project, "x.dep.okHttp.okHttp4"))
  implementation(groovy.util.Eval.x(project, "x.dep.moshi.moshi"))
  kapt(groovy.util.Eval.x(project, "x.dep.moshi.kotlinCodegen"))

  testImplementation(groovy.util.Eval.x(project, "x.dep.junit"))
  testImplementation(groovy.util.Eval.x(project, "x.dep.truth"))

  testImplementation(groovy.util.Eval.x(project, "x.dep.okHttp.mockWebServer4"))
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
