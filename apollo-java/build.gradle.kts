plugins {
  id("org.jetbrains.kotlin.jvm")
  id("apollo.library")
}

apolloLibrary {
  javaModuleName("com.apollographql.apollo3.java")
}

dependencies {
  api(projects.apolloApi)
  implementation(projects.apolloRuntime)

  testImplementation(projects.apolloMockserver)
  testImplementation(libs.kotlin.test.junit)
}
