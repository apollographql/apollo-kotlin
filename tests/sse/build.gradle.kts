plugins {
  id("org.jetbrains.kotlin.jvm")

}

dependencies {
  implementation("com.apollographql.apollo3:apollo-runtime")

  testImplementation(projects.sseMockserver)
  testImplementation(groovy.util.Eval.x(project, "x.dep.kotlinJunit"))

}
