plugins {
  id("org.jetbrains.kotlin.jvm")
  id("com.apollographql.apollo3")
}

dependencies {
  implementation("com.apollographql.apollo3:apollo-runtime")
  implementation("com.apollographql.apollo3:apollo-mockserver")
  implementation("com.apollographql.apollo3:apollo-normalized-cache")
  implementation("com.apollographql.apollo3:apollo-testing-support")
  testImplementation(groovy.util.Eval.x(project, "x.dep.kotlinJunit"))
  testImplementation(groovy.util.Eval.x(project, "x.dep.junit"))
}

apollo {
  service("1") {
    sourceFolder.set("1")
    packageName.set("com.example.one")
  }
  service("2") {
    sourceFolder.set("2")
    packageName.set("com.example.two")
  }
}
