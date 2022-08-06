plugins {
  id("org.jetbrains.kotlin.jvm")
  id("com.apollographql.apollo3.external")
}

dependencies {
  implementation(libs.apollo.runtime)
  testImplementation(libs.kotlin.test)
  testImplementation(libs.junit)
}

apollo {
  service("default") {
    srcDir("graphql")
    packageName.set("default")
  }
  service("none") {
    srcDir("graphql")
    requiresOptInAnnotation.set("none")
    packageName.set("none")
  }
  service("custom") {
    srcDir("graphql")
    requiresOptInAnnotation.set("com.example.MyRequiresOptIn")
    packageName.set("custom")
  }
}
