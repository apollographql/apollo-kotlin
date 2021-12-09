plugins {
  id("org.jetbrains.kotlin.jvm")
  id("org.jetbrains.kotlin.plugin.spring")
  id("application")
}

dependencies {
  api(groovy.util.Eval.x(project, "x.dep.graphqlKotlin"))
  api(groovy.util.Eval.x(project, "x.dep.kotlin.reflect").toString()) {
    because("graphqlKotlin pull kotlin-reflect and that triggers a warning like" +
        "Runtime JAR files in the classpath should have the same version.")
  }
  implementation(groovy.util.Eval.x(project, "x.dep.kotlin.coroutinesReactor").toString()) {
    because("reactor must have the same version as the coroutines version")
  }
}

application {
  mainClass.set("com.apollographql.apollo.sample.server.MainKt")
}