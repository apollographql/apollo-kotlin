plugins {
  id("org.jetbrains.kotlin.jvm")
  id("com.apollographql.apollo3")
}

apolloTest()

dependencies {
  implementation(libs.apollo.runtime)
  testImplementation(libs.kotlin.test)
  testImplementation(libs.junit)
}

apollo {
  service("default") {
    srcDir("graphql")
    packageName.set("default")
    languageVersion.set("1.5")
  }
  service("none") {
    srcDir("graphql")
    requiresOptInAnnotation.set("none")
    packageName.set("none")
    languageVersion.set("1.5")
  }
  service("custom") {
    srcDir("graphql")
    requiresOptInAnnotation.set("com.example.MyRequiresOptIn")
    packageName.set("custom")
    languageVersion.set("1.5")
  }
}

