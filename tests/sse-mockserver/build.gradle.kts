plugins {
  id("org.jetbrains.kotlin.jvm")
  id("application")
}

dependencies {

}

application {
  mainClass.set("com.apollographql.apollo.sample.server.sse.MainKt")
}
