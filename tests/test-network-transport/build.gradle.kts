plugins {
  id("org.jetbrains.kotlin.jvm")
  id("com.apollographql.apollo3")
}

dependencies {
  implementation("com.apollographql.apollo3:apollo-runtime")
  implementation("com.apollographql.apollo3:apollo-mockserver")
  testImplementation(groovy.util.Eval.x(project, "x.dep.kotlinJunit"))
  testImplementation(groovy.util.Eval.x(project, "x.dep.junit"))
  testImplementation("com.apollographql.apollo3:apollo-testing-support")
}

apollo {
  packageName.set("testnetworktransport")
  generateTestBuilders.set(true)
}
