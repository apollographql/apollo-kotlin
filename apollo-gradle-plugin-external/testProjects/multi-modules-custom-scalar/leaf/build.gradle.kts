plugins {
  kotlin("jvm")
  id("com.apollographql.apollo3")
  id("application")
}

dependencies {
  implementation(kotlin("stdlib"))
  implementation(groovy.util.Eval.x(project, "x.dep.apollo.api"))
  testImplementation(kotlin("test-junit"))

  implementation(project(":root"))
  apolloMetadata(project(":root"))
}

application {
  mainClass.set("LeafKt")
}

apollo {
  packageName.set("com.library")
}
