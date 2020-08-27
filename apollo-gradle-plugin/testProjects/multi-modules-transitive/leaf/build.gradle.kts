plugins {
  kotlin("jvm")
  id("com.apollographql.apollo")
  id("application")
}

dependencies {
  implementation(groovy.util.Eval.x(project, "x.dep.apollo.api"))

  implementation(kotlin("stdlib"))
  testImplementation(kotlin("test-junit"))

  implementation(project(":node"))
  apollo(project(":node"))
}

application {
  mainClass.set("LeafKt")
}

apollo {
  generateKotlinModels.set(true)
}