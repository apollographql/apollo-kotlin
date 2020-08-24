plugins {
  kotlin("jvm")
  id("com.apollographql.apollo")
  id("maven-publish")
}

dependencies {
  implementation(kotlin("stdlib"))
  implementation(groovy.util.Eval.x(project, "x.dep.apollo.api"))

  testImplementation(kotlin("test-junit"))
}

apollo {
  generateApolloMetadata.set(true)
  generateKotlinModels.set(true)
  customTypeMapping.set(mapOf("Date" to "java.util.Date"))
}
