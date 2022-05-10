plugins {
  id("org.jetbrains.kotlin.jvm")
  id("com.apollographql.apollo3.external")
}

dependencies {
  implementation("com.apollographql.apollo3:apollo-runtime")
  testImplementation(groovy.util.Eval.x(project, "x.dep.kotlinJunit"))
  testImplementation(groovy.util.Eval.x(project, "x.dep.junit"))
}

apollo {
  service("default") {
    srcDir("graphql")
    packageName.set("default")
  }
  service("none") {
    srcDir("graphql")
    experimentalAnnotation.set("none")
    packageName.set("none")
  }
  service("custom") {
    srcDir("graphql")
    experimentalAnnotation.set("com.example.MyExperimental")
    packageName.set("custom")
  }
}