plugins {
  id("java")
  id("com.apollographql.apollo3")
}

apolloTest()

dependencies {
  implementation(libs.apollo.runtime.java)
  implementation(libs.apollo.mockserver)
  implementation(libs.apollo.rx3.java)
  testImplementation(libs.junit)
  testImplementation(libs.truth)
  testImplementation(project(":sample-server"))
}

apollo {
  service("main") {
    schemaFiles.from(file("../sample-server/src/main/resources/schema.graphqls"))
    srcDir("src/main/graphql/main")
    packageName.set("javatest")
    generateModelBuilders.set(true)
  }

  service("scalars") {
    srcDir("src/main/graphql/scalars")
    mapScalarToJavaString("LanguageCode")
    mapScalarToJavaObject("Json")
    mapScalarToJavaLong("Long")
    mapScalar("GeoPoint", "scalar.GeoPoint")
    packageName.set("scalars")
    generateModelBuilders.set(true)
  }

  service("graphql-ws") {
    srcDir("src/main/graphql/graphql-ws")
    packageName.set("graphqlws")
    generateModelBuilders.set(true)
  }

  service("appsync") {
    srcDir("src/main/graphql/appsync")
    packageName.set("appsync")
    generateModelBuilders.set(true)
  }

  service("batching") {
    srcDir("src/main/graphql/batching")
    packageName.set("batching")
    generateModelBuilders.set(true)
  }

}
