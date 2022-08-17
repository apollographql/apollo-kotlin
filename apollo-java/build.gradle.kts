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
  api(projects.apolloNormalizedCacheSqlite) {
    // apollo-runtime is an api dependency in apollo-normalized-cache, but we want it to be an implementation dependency
    exclude(group = "com.apollographql.apollo3", module = "apollo-runtime")
  }

  testImplementation(projects.apolloMockserver)
  testImplementation(libs.kotlin.test.junit)
}
