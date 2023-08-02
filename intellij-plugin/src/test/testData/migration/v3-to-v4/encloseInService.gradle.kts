dependencies {
  implementation("org.example:somelibrary:1.0.0")
}

apollo {
  packageName.set("com.example")
  // Some comment
  codegenModels.set("operationBased")
  linkSqlite.set(true)
  srcDir("src/main/graphql")
}

apollo {
  service("zgluteks") {
    packageName.set("com.example")
    codegenModels.set("responseBased")
  }
}
