plugins {
  id("org.jetbrains.kotlin.jvm")
  id("com.apollographql.apollo3")
}

apolloTest()

dependencies {
  implementation(libs.apollo.runtime)
  implementation(project(":sample-server"))
  implementation(libs.apollo.testingsupport)
  testImplementation(libs.kotlin.test)
  testImplementation(libs.junit)
  testImplementation(libs.okhttp)
}

apollo {
  service("service") {
    generateDataBuilders.set(true)
    packageName.set("com.example")
    schemaFiles.from(file("../sample-server/src/main/resources/schema.graphqls"))
  }
}
