plugins {
  id("org.jetbrains.kotlin.jvm")
  id("com.apollographql.apollo3")
}

dependencies {
  implementation("com.apollographql.apollo3:apollo-runtime")
  implementation("com.apollographql.apollo3:apollo-normalized-cache")
  testImplementation("com.apollographql.apollo3:apollo-testing-support")
  testImplementation(libs.kotlin.test)
  testImplementation(libs.turbine)
}

apollo {
  packageName.set("optimistic")
}
