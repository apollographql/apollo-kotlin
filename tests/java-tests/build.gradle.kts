plugins {
  id("com.apollographql.apollo3")
  id("java")
}

dependencies {
  implementation("com.apollographql.apollo3:apollo-runtime")
  implementation("com.apollographql.apollo3:apollo-http-cache")
  implementation("com.apollographql.apollo3:apollo-normalized-cache")
  implementation("com.apollographql.apollo3:apollo-normalized-cache-sqlite")
  implementation("com.apollographql.apollo3:apollo-mockserver")
  implementation("com.apollographql.apollo3:apollo-rx2-support")
  testImplementation(libs.junit)
  testImplementation(libs.truth)
}

apollo {
  packageName.set("javatest")
}
