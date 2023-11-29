plugins {
  id("org.jetbrains.kotlin.jvm")
  id("com.apollographql.apollo3")
}

apolloTest()

dependencies {
  implementation(libs.apollo.api)
  implementation(libs.apollo.testingsupport)
  testImplementation(libs.kotlin.test)
  testImplementation(libs.junit)
  testImplementation(libs.okhttp)
}

apollo {
  service("default") {
    srcDir("src/main/graphql/shared")
    srcDir("src/main/graphql/throw")
    packageName.set("default")
  }
  service("null") {
    srcDir("src/main/graphql/shared")
    srcDir("src/main/graphql/null")
    packageName.set("null")
  }
  service("result") {
    srcDir("src/main/graphql/shared")
    srcDir("src/main/graphql/result")
    packageName.set("result")
  }
}
