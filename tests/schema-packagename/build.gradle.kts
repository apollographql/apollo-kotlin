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

    // This is to force running without a worker. See https://github.com/gradle/gradle/issues/28147
    operationIdGenerator.set(object: OperationIdGenerator {
      override val version: String
        get() = "v1"

      override fun apply(operationDocument: String, operationName: String): String {
        return operationName
      }
    })
  }
}
