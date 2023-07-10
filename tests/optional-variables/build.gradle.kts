plugins {
  id("org.jetbrains.kotlin.jvm")
  id("apollo.test")
  id("com.apollographql.apollo3")
}

dependencies {
  implementation(libs.apollo.runtime)
  testImplementation(libs.kotlin.test)
  testImplementation(libs.junit)
}

apollo {
  service("service") {
    packageName.set("optional.variables")
    generateOptionalOperationVariables.set(false)
    generateFragmentImplementations.set(true)
  }
}
