plugins {
  id("org.jetbrains.kotlin.jvm")
  id("com.apollographql.apollo")
}

apolloTest()

dependencies {
  implementation(libs.apollo.api)
  implementation(project(":sample-server"))
  implementation(libs.apollo.mockserver)
  testImplementation(libs.junit)
  testImplementation(libs.okhttp)
}

apollo {
  service("service") {
    packageName.set("com.example")
    schemaFiles.from(file("../sample-server/graphql/schema.graphqls"))
  }
}
