dependencies {
  implementation("org.example:somelibrary:1.0.0")
}

apollo {
  service("a") { // Should highlight
    packageName.set("com.example")
    // Some comment
    codegenModels.set("operationBased")
    srcDir("src/main/graphql")
  }

  service("c") {
    packageName.set("com.example")
    // Some comment
    codegenModels.set("operationBased")
    srcDir("src/main/graphql")
    introspection {
      endpointUrl.set("https://example.com/graphql")
      schemaFiles.from(file("src/main/graphql/schema.graphqls"))
    }
  }

  service("d") { // Should highlight
    packageName.set("com.example")
    // Some comment
    codegenModels.set("operationBased")
    srcDir("src/main/graphql")
    registry {
      key.set(System.getenv("APOLLO_KEY"))
      graph.set(System.getenv("APOLLO_GRAPH"))
      schemaFiles.from(file("src/main/graphql/schema.graphqls"))
    }
  }
}
