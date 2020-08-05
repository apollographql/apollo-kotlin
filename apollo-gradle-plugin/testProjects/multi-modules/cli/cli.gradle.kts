plugins {
  kotlin("jvm")
  id("com.apollographql.apollo")
  id("application")
}

dependencies {
  implementation(kotlin("stdlib"))
  implementation(groovy.util.Eval.x(project, "x.dep.apollo.api"))
  testImplementation(kotlin("test-junit"))

  implementation(project(":lib"))
  apollo(project(":lib"))
}

application {
  mainClass.set("MainCliKt")
}

apollo {
  generateKotlinModels.set(true)
}