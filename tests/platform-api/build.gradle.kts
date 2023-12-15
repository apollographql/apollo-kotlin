plugins {
  id("org.jetbrains.kotlin.jvm")
  id("com.apollographql.apollo3")
}

apolloTest()

dependencies {
  implementation("com.apollographql.apollo3:apollo-runtime")
}

apollo {
  service("service") {
    packageName.set("com.example")

    val apiKey = System.getenv("PLATFORM_API_TESTS_KEY")
    if (key != null) {
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
