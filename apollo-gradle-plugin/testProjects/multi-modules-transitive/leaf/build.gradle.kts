plugins {
  kotlin("jvm")
  id("com.apollographql.apollo3")
  id("application")
}

dependencies {
  implementation(groovy.util.Eval.x(project, "x.dep.apollo.api"))

  implementation(kotlin("stdlib"))
  testImplementation(kotlin("test-junit"))

  implementation(project(":node"))
  apolloMetadata(project(":node"))
}

application {
  mainClass.set("LeafKt")
}

apollo {
  packageNamesFromFilePaths()
}