plugins {
  kotlin("jvm")
  id("com.apollographql.apollo3")
  id("maven-publish")
}

dependencies {
  implementation(kotlin("stdlib"))
  implementation(groovy.util.Eval.x(project, "x.dep.apollo.api"))

  testImplementation(kotlin("test-junit"))
}

apollo {
  packageNamesFromFilePaths()
  generateApolloMetadata.set(true)
  customScalarsMapping.set(mapOf("Date" to "java.util.Date"))
}
