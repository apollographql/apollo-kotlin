plugins {
  alias(libs.plugins.kotlin.jvm)
  alias(libs.plugins.apollo)
}

dependencies {
  add("implementation", libs.apollo.api)
}

apollo {
  service("service") {
    packageName.set("com.example")

    registry {
      key.set("<key>")
      graph.set("<graph>")
      graphVariant.set("main")
      schemaFile.set(file("src/main/graphql/schema.graphqls"))
    }

    operationManifestFormat.set("persistedQueryManifest")
    registerOperations {
      key.set("<key>")
      graph.set("<graph>")
      graphVariant.set("main")
      listId.set("<PQListId>")
    }
  }
}
