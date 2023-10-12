plugins {
  id("org.jetbrains.kotlin.jvm")
  id("com.apollographql.apollo3")
}

apolloTest()

dependencies {
  implementation("com.apollographql.apollo3:apollo-runtime")
}

apollo {
  service("main") {
    packageName.set("com.example.generated")
    languageVersion.set("1.5")
  }
}
