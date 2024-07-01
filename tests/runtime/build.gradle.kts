import com.apollographql.apollo.annotations.ApolloExperimental

plugins {
  id("org.jetbrains.kotlin.jvm")
  id("com.apollographql.apollo")
}

apolloTest()

dependencies {
  implementation(libs.apollo.runtime)
  implementation(project(":sample-server"))
  testImplementation(libs.apollo.testingsupport)
  testImplementation(libs.apollo.mockserver)
  testImplementation(libs.kotlin.test)
  testImplementation(libs.junit)
  testImplementation(libs.okhttp)
}

apollo {
  service("service") {
    @OptIn(ApolloExperimental::class)
    generateDataBuilders.set(true)
    packageName.set("com.example")
    schemaFiles.from(file("../sample-server/src/main/resources/schema.graphqls"))
  }
}
