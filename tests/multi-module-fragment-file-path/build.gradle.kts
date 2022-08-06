plugins {
  id("com.apollographql.apollo3")
  id("org.jetbrains.kotlin.jvm")
}

dependencies {
  implementation(libs.apollo.runtime)
  implementation(projects.multiModuleRoot)
  apolloMetadata(projects.multiModuleRoot)
  testImplementation(libs.kotlin.test.junit)
}

apollo {
  packageNamesFromFilePaths()
}
