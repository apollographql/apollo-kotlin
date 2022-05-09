plugins {
  application
  kotlin("jvm")
  kotlin("plugin.serialization") version "1.5.20"
}

dependencies {

  ///////////////////////////
  // implementation

  // ktor
  implementation(groovy.util.Eval.x(project, "x.dep.ktor.serverCore"))
  implementation(groovy.util.Eval.x(project, "x.dep.ktor.serverNetty"))
  implementation(groovy.util.Eval.x(project, "x.dep.ktor.serialization"))
  implementation(groovy.util.Eval.x(project, "x.dep.logback"))

  ////////////////////////////
  // test
  testImplementation(groovy.util.Eval.x(project, "x.dep.kotlinJunit"))
  testImplementation(groovy.util.Eval.x(project, "x.dep.ktor.clientOkHttp"))

}

application {
  applicationName = "GraphQl SSE Subscriptions server in Ktor"
  mainClass.set("com.apollographql.apollo.sample.server.MainKt")
  applicationDefaultJvmArgs = listOf("-Dio.ktor.development=true") // hot reload when running gradle in continuous mode
}
