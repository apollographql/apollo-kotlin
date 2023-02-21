plugins {
  id("org.jetbrains.kotlin.jvm")
  id("apollo.test")
  id("com.apollographql.apollo3")
}

dependencies {
  implementation(golatac.lib("apollo.runtime"))
  implementation(golatac.lib("apollo.httpCache"))
  implementation(golatac.lib("apollo.normalizedcache"))
  implementation(golatac.lib("apollo.mockserver"))
  testImplementation(golatac.lib("kotlin.test.junit"))
  testImplementation(golatac.lib("apollo.testingsupport"))
}

apollo {
  service("service") {
    packageName.set("com.example")
    generateDataBuilders.set(true)
  }
}
