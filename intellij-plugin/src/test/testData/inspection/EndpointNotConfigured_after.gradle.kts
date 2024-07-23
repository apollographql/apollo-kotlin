dependencies {
  implementation("org.example:somelibrary:1.0.0")
}

apollo {
  service("a") { // Should highlight
    packageName.set("com.example")
    // Some comment
    codegenModels.set("operationBased")
    srcDir("src/main/graphql")
    introspection {
      endpointUrl.set("https://example.com/graphql")
      headers.put("api-key", "1234567890abcdef")
      schemaFile.set(file("src/main/graphql/schema.graphqls"))
    }
  }

  service("c") {
    packageName.set("com.example")
    // Some comment
    codegenModels.set("operationBased")
    srcDir("src/main/graphql")
    introspection {
      endpointUrl.set("https://example.com/graphql")
      schemaFile.set(file("src/main/graphql/schema.graphqls"))
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
      schemaFile.set(file("src/main/graphql/schema.graphqls"))
    }
    introspection {
      endpointUrl.set("https://example.com/graphql")
      headers.put("api-key", "1234567890abcdef")
      schemaFile.set(file("src/main/graphql/schema.graphqls"))
    }
  }

  service("e") { // Shouldn't highlight
    packageName.set("com.example")
    dependsOn(":schemaModule", bidirectional = true)
  }
}
