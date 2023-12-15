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

    registry {
      key.set(System.getenv("PLATFORM_API_TESTS_KEY"))
      graph.set("changeme")
      graphVariant.set("current")
      schemaFile.set(file("src/main/graphql/schema.graphqls"))
    }

    operationManifestFormat.set("persistedQueryManifest")
    registerOperations {
      key.set(System.getenv("PLATFORM_API_TESTS_KEY"))
      graph.set("changeme")
      graphVariant.set("current")
      listId.set("changeme")
    }
  }
}
