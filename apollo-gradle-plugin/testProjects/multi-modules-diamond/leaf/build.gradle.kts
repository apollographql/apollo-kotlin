plugins {
  kotlin("jvm")
  id("com.apollographql.apollo3")
  id("application")
}

dependencies {
  implementation(kotlin("stdlib"))
  testImplementation(kotlin("test-junit"))
  implementation(groovy.util.Eval.x(project, "x.dep.apollo.api"))

  implementation(project(":node1"))
  implementation(project(":node2"))

  apolloMetadata(project(":node1"))
  apolloMetadata(project(":node2"))
}

application {
  mainClass.set("LeafKt")
}

apollo {
  packageNamesFromFilePaths()
}
