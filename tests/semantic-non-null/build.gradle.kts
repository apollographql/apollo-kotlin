plugins {
  id("org.jetbrains.kotlin.jvm")
  id("com.apollographql.apollo")
}

apolloTest()

dependencies {
  implementation(libs.apollo.api)
  implementation(libs.apollo.testingsupport.internal)
  testImplementation(libs.junit)
  testImplementation(libs.okhttp)
}

apollo {
  service("service") {
    srcDir("src/main/graphql/")
    packageName.set("com.example")
  }
}
