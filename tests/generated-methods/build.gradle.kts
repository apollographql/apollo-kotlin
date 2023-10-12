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
  service("service") {
    packageName.set("generatedMethods")
    languageVersion.set("1.5")
    generateDataBuilders.set(true)
    generateMethods.set(listOf("toString", "equalsHashCode", "copy"))
  }
}
