import com.android.build.gradle.internal.tasks.factory.dependsOn
import com.apollographql.apollo.gradle.internal.ApolloPushSchemaTask

plugins {
  id("org.jetbrains.kotlin.jvm")
  id("com.apollographql.apollo")
}

apolloTest()

dependencies {
  implementation(libs.apollo.api)
  implementation(libs.apollo.tooling)
  testImplementation(libs.kotlin.test)
  testImplementation(libs.apollo.testingsupport)
  testImplementation(libs.apollo.mockserver)
}

val apiKey = System.getenv("PLATFORM_API_TESTS_KEY")

apollo {
  service("service") {
    packageName.set("com.example")

    if (apiKey != null) {
      registry {
        key.set(apiKey)
        graph.set("Apollo-Kotlin-CI-tests")
        graphVariant.set("current")
        schemaFile.set(file("src/main/graphql/schema.graphqls"))
      }

      operationManifestFormat.set("persistedQueryManifest")
      registerOperations {
        key.set(apiKey)
        graph.set("Apollo-Kotlin-CI-tests")
        graphVariant.set("current")
        listId.set("2ad55629-f473-4d72-9897-2dd2096540f4")
      }
    }
  }
}

if (apiKey != null) {
  tasks.named<ApolloPushSchemaTask>("pushApolloSchema") {
    key.set(apiKey)
    graph.set("Apollo-Kotlin-CI-tests")
    subgraph.set("subgraph1")
    schema.set("platform-api/src/main/graphql/schema.graphqls")
    revision.set("1")
    graphVariant.set("current")
  }

  tasks.named("generateServiceApolloSources").dependsOn("downloadServiceApolloSchemaFromRegistry")

  tasks.register("platformApiTests") {
    description = "Execute Platform API tests"
    dependsOn("registerServiceApolloOperations")
    dependsOn("downloadServiceApolloSchemaFromRegistry")
    dependsOn("pushApolloSchema")
    dependsOn("test")
  }
}
