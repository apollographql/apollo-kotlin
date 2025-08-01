plugins {
  id("org.jetbrains.kotlin.jvm")
  id("com.apollographql.apollo")
}

apolloTest()

dependencies {
  implementation(libs.apollo.api)
}

apollo {
  service("service") {
    schemaFiles.from(fileTree("src/main/graphql/").apply {
      include("com/example/schema.graphqls")
    })
    packageNamesFromFilePaths()
  }
}
