plugins {
  id("org.jetbrains.kotlin.jvm")
  id("com.apollographql.apollo3")
}

apolloTest()

dependencies {
  implementation(libs.apollo.api)
  testImplementation(libs.kotlin.test)
}

apollo {
  service("service") {
    schemaFile.set(file("src/main/graphql/com/example/schema.graphqls"))
    packageNamesFromFilePaths()
  }
}
