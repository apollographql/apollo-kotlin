import com.apollographql.apollo3.compiler.OperationIdGenerator

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
    schemaFiles.from(fileTree("src/main/graphql/").apply {
      include("com/example/schema.graphqls")
    })
    packageNamesFromFilePaths()
  }
}
