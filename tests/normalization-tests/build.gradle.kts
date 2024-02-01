plugins {
  id("org.jetbrains.kotlin.jvm")
  id("com.apollographql.apollo3")
}

apolloTest()

dependencies {
  implementation(libs.apollo.runtime)
  implementation(libs.apollo.mockserver)
  implementation(libs.apollo.normalizedcache)
  implementation(libs.apollo.testingsupport)
  testImplementation(libs.kotlin.test)
  testImplementation(libs.junit)
}

apollo {
  service("1") {
    srcDir("src/main/graphql/1")
    packageName.set("com.example.one")
  }
  service("2") {
    srcDir("src/main/graphql/2")
    packageName.set("com.example.two")
  }
  service("3") {
    srcDir("src/main/graphql/3")
    packageName.set("com.example.three")
  }
}
