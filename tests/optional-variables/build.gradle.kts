plugins {
  kotlin("jvm")
  id("com.apollographql.apollo3")
}

dependencies {
  implementation(libs.apollo.runtime)
  testImplementation(libs.kotlin.test)
  testImplementation(libs.junit)
}

apollo {
  packageName.set("optional.variables")
  generateOptionalOperationVariables.set(false)
  generateFragmentImplementations.set(true)
}
