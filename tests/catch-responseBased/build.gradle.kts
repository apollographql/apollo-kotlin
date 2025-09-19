plugins {
  id("org.jetbrains.kotlin.jvm")
  id("com.apollographql.apollo")
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
  service("throw") {
    srcDir("src/main/graphql/shared")
    srcDir("src/main/graphql/throw")
    packageName.set("throw")
    codegenModels.set("responseBased")
  }
  service("null") {
    srcDir("src/main/graphql/shared")
    srcDir("src/main/graphql/null")
    packageName.set("null")
    codegenModels.set("responseBased")
  }
  service("result") {
    srcDir("src/main/graphql/shared")
    srcDir("src/main/graphql/result")
    packageName.set("result")
    codegenModels.set("responseBased")
  }
}
