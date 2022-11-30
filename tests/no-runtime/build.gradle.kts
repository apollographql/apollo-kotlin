plugins {
  id("org.jetbrains.kotlin.jvm")
  id("apollo.test")
  id("com.apollographql.apollo3")
}

dependencies {
  implementation(golatac.lib("apollo.api"))
  implementation(project(":sample-server"))
  implementation(golatac.lib("apollo.testingsupport"))
  testImplementation(golatac.lib("kotlin.test"))
  testImplementation(golatac.lib("junit"))
  testImplementation(golatac.lib("okhttp"))
}

apollo {
  service("service") {
    packageName.set("com.example")
  }
}
