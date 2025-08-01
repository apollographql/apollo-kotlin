plugins {
  id("org.jetbrains.kotlin.jvm")
  id("com.apollographql.apollo")
}

apolloTest()

dependencies {
  implementation(libs.apollo.runtime)
  testImplementation(libs.junit)
}

apollo {
  service("service") {
    packageName.set("reserved")
  }
}

tasks.withType(Test::class.java).configureEach {
  failOnNoDiscoveredTests.set(false)
}