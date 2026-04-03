plugins {
  id("org.jetbrains.kotlin.jvm")
  id("com.apollographql.apollo")
}

apolloTest()

dependencies {
  implementation(libs.apollo.api)
  testImplementation(libs.junit)
}

apollo {
  service("service") {
    packageName.set("com.example")
    codegenModels.set("responseBased")
  }
}
