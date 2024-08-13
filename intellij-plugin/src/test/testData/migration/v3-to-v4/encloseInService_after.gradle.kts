dependencies {
  implementation("org.example:somelibrary:1.0.0")
}

apollo {
  linkSqlite.set(true)
  service("service") {
    packageName.set("com.example")
    // Some comment
    codegenModels.set("operationBased")
    srcDir("src/main/graphql")
  }
}

apollo {
  service("zgluteks") {
    packageName.set("com.example")
    codegenModels.set("responseBased")
  }
}
