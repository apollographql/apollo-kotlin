plugins {
  kotlin("jvm")
  id("com.apollographql.apollo3")
}

dependencies {
  implementation(kotlin("stdlib"))
  implementation(groovy.util.Eval.x(project, "x.dep.apolloApi"))
  testImplementation(kotlin("test-junit"))

  implementation(project(":root"))
  apolloMetadata(project(":root"))
}

apollo {
  packageName.set("com.library")
  mapScalar("Long", "java.lang.Long")
}
