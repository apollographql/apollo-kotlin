plugins {
  kotlin("jvm")
  id("com.apollographql.apollo3")
  id("maven-publish")
}

dependencies {
  implementation(kotlin("stdlib"))
  implementation(groovy.util.Eval.x(project, "x.dep.apolloApi"))

  testImplementation(libs.kotlin.test.junit)
}

apollo {
  alwaysGenerateTypesMatching.set(listOf("Cat"))
  packageNamesFromFilePaths()
  generateApolloMetadata.set(true)
  customScalarsMapping.set(mapOf("Date" to "java.util.Date"))
}
