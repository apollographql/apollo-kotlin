plugins {
  id("org.jetbrains.kotlin.jvm")
  id("apollo.test")
  id("com.apollographql.apollo3")
}

dependencies {
  implementation(libs.apollo.api)
}

apollo {
  service("service") {
    packageName.set("com.example")
  }
}
